const functions = require("firebase-functions");
const admin = require("firebase-admin");
const axios = require("axios");
const { v4: uuidv4 } = require("uuid");

admin.initializeApp();
const db = admin.firestore();

// ==============================
// Конфигурация
// ==============================
// Установить через: firebase functions:config:set yookassa.shop_id="YOUR_SHOP_ID" yookassa.secret_key="YOUR_SECRET_KEY"
// Или через .env файл для Functions v2

const YOOKASSA_API = "https://api.yookassa.ru/v3";
const PREMIUM_PRICE = "199.00";
const PREMIUM_CURRENCY = "RUB";
const PREMIUM_DURATION_DAYS = 30;

// ==============================
// Утилиты
// ==============================

/**
 * Проверяет Firebase ID token и возвращает uid
 */
async function verifyToken(idToken) {
  const decoded = await admin.auth().verifyIdToken(idToken);
  return decoded.uid;
}

/**
 * Активирует Premium для пользователя в Firestore
 */
async function activatePremium(uid, source, paymentId) {
  const expiresAt = Date.now() + PREMIUM_DURATION_DAYS * 24 * 60 * 60 * 1000;
  await db.collection("users").doc(uid).collection("subscription").doc("status").set({
    premium: true,
    source: source,
    expiresAt: expiresAt,
    purchaseToken: paymentId,
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  }, { merge: true });
  return expiresAt;
}

/**
 * Проверяет, не истекла ли подписка
 */
async function checkPremiumActive(uid) {
  const doc = await db.collection("users").doc(uid)
    .collection("subscription").doc("status").get();
  if (!doc.exists) return false;
  const data = doc.data();
  return data.premium === true && (data.expiresAt || 0) > Date.now();
}

// ==============================
// API: Создание платежа ЮKassa
// ==============================

exports.api = functions.https.onRequest(async (req, res) => {
  // CORS
  res.set("Access-Control-Allow-Origin", "*");
  res.set("Access-Control-Allow-Methods", "POST, OPTIONS");
  res.set("Access-Control-Allow-Headers", "Content-Type");

  if (req.method === "OPTIONS") {
    return res.status(204).send("");
  }

  const path = req.path;

  // ---- POST /api/createPayment ----
  if (path === "/createPayment" && req.method === "POST") {
    try {
      const { uid, idToken } = req.body;

      if (!uid || !idToken) {
        return res.status(400).json({ error: "Не указаны uid или idToken" });
      }

      // Верифицируем токен
      const verifiedUid = await verifyToken(idToken);
      if (verifiedUid !== uid) {
        return res.status(403).json({ error: "Неверный токен авторизации" });
      }

      // Проверяем, нет ли уже активного Premium
      const isActive = await checkPremiumActive(uid);
      if (isActive) {
        return res.status(400).json({ error: "Premium уже активен" });
      }

      // Получаем конфигурацию ЮKassa
      const shopId = functions.config().yookassa?.shop_id;
      const secretKey = functions.config().yookassa?.secret_key;

      if (!shopId || !secretKey) {
        console.error("ЮKassa credentials not configured");
        return res.status(500).json({ error: "Платёжная система не настроена" });
      }

      // Создаём платёж в ЮKassa
      const idempotenceKey = uuidv4();
      const returnUrl = `https://${req.hostname}/success.html`;

      const payment = await axios.post(
        `${YOOKASSA_API}/payments`,
        {
          amount: {
            value: PREMIUM_PRICE,
            currency: PREMIUM_CURRENCY,
          },
          confirmation: {
            type: "redirect",
            return_url: returnUrl,
          },
          capture: true,
          description: `Quarter Premium — 1 месяц (uid: ${uid})`,
          metadata: {
            uid: uid,
            product: "quarter_premium_monthly",
          },
        },
        {
          auth: { username: shopId, password: secretKey },
          headers: {
            "Idempotence-Key": idempotenceKey,
            "Content-Type": "application/json",
          },
        }
      );

      const paymentData = payment.data;

      // Сохраняем pending-платёж в Firestore
      await db.collection("payments").doc(paymentData.id).set({
        uid: uid,
        amount: PREMIUM_PRICE,
        currency: PREMIUM_CURRENCY,
        status: paymentData.status,
        yookassaId: paymentData.id,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
      });

      return res.json({
        paymentId: paymentData.id,
        confirmationUrl: paymentData.confirmation.confirmation_url,
      });
    } catch (err) {
      console.error("createPayment error:", err.response?.data || err.message);
      return res.status(500).json({ error: "Не удалось создать платёж" });
    }
  }

  // ---- POST /api/yookassaWebhook ----
  if (path === "/yookassaWebhook" && req.method === "POST") {
    try {
      const event = req.body;

      if (event.event !== "payment.succeeded") {
        return res.status(200).json({ ok: true });
      }

      const payment = event.object;
      const uid = payment.metadata?.uid;
      const paymentId = payment.id;

      if (!uid) {
        console.error("Webhook: no uid in metadata", paymentId);
        return res.status(200).json({ ok: true });
      }

      // Активируем Premium
      const expiresAt = await activatePremium(uid, "web", paymentId);

      // Обновляем статус платежа
      await db.collection("payments").doc(paymentId).update({
        status: "succeeded",
        paidAt: admin.firestore.FieldValue.serverTimestamp(),
        expiresAt: expiresAt,
      });

      console.log(`Premium activated for ${uid}, expires ${new Date(expiresAt).toISOString()}`);
      return res.status(200).json({ ok: true });
    } catch (err) {
      console.error("yookassaWebhook error:", err.message);
      return res.status(500).json({ error: err.message });
    }
  }

  // ---- POST /api/verifyGooglePurchase ----
  if (path === "/verifyGooglePurchase" && req.method === "POST") {
    try {
      const { uid, idToken, purchaseToken, productId } = req.body;

      if (!uid || !idToken || !purchaseToken) {
        return res.status(400).json({ error: "Missing parameters" });
      }

      const verifiedUid = await verifyToken(idToken);
      if (verifiedUid !== uid) {
        return res.status(403).json({ error: "Invalid token" });
      }

      // Верификация через Google Play Developer API
      // Требует настройки Service Account с доступом к Google Play Console
      // const { google } = require('googleapis');
      // const androidpublisher = google.androidpublisher('v3');
      // const result = await androidpublisher.purchases.subscriptions.get({...});

      // Пока активируем по факту получения токена от клиента
      // В продакшене заменить на полную верификацию через Google Play Developer API
      await activatePremium(uid, "google", purchaseToken);

      return res.json({ premium: true, source: "google" });
    } catch (err) {
      console.error("verifyGooglePurchase error:", err.message);
      return res.status(500).json({ error: "Verification failed" });
    }
  }

  // ---- POST /api/verifyRuStorePurchase ----
  if (path === "/verifyRuStorePurchase" && req.method === "POST") {
    try {
      const { uid, idToken, purchaseToken, productId } = req.body;

      if (!uid || !idToken || !purchaseToken) {
        return res.status(400).json({ error: "Missing parameters" });
      }

      const verifiedUid = await verifyToken(idToken);
      if (verifiedUid !== uid) {
        return res.status(403).json({ error: "Invalid token" });
      }

      // Верификация через RuStore API
      // Документация: https://help.rustore.ru/rustore/for_developers/developer-documentation/sdk/payments
      // В продакшене заменить на проверку через RuStore Server API

      await activatePremium(uid, "rustore", purchaseToken);

      return res.json({ premium: true, source: "rustore" });
    } catch (err) {
      console.error("verifyRuStorePurchase error:", err.message);
      return res.status(500).json({ error: "Verification failed" });
    }
  }

  // ---- GET /api/checkPremium ----
  if (path === "/checkPremium" && req.method === "POST") {
    try {
      const { uid, idToken } = req.body;

      const verifiedUid = await verifyToken(idToken);
      if (verifiedUid !== uid) {
        return res.status(403).json({ error: "Invalid token" });
      }

      const doc = await db.collection("users").doc(uid)
        .collection("subscription").doc("status").get();

      if (!doc.exists) {
        return res.json({ premium: false });
      }

      const data = doc.data();
      const isActive = data.premium === true && (data.expiresAt || 0) > Date.now();

      return res.json({
        premium: isActive,
        source: data.source || null,
        expiresAt: data.expiresAt || null,
      });
    } catch (err) {
      console.error("checkPremium error:", err.message);
      return res.status(500).json({ error: err.message });
    }
  }

  return res.status(404).json({ error: "Not found" });
});
