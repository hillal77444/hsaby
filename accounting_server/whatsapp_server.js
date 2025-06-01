const express = require('express');
const { Client, LocalAuth, MessageMedia } = require('whatsapp-web.js');
const qrcode = require('qrcode');
const path = require('path');
const fs = require('fs');
const { exec } = require('child_process');
const util = require('util');
const execPromise = util.promisify(exec);
const fetch = require('node-fetch');

const app = express();
const port = 3003;
const LOG_FILE = path.join(__dirname, 'whatsapp_api.log');
const SESSION_FILE = path.join(__dirname, 'whatsapp_sessions.json');
const AUTH_DIR = path.join(__dirname, '.wwebjs_auth');

// إعداد السجلات
function log(message) {
  const timestamp = new Date().toISOString();
  const logMessage = `[${timestamp}] ${message}\n`;
  console.log(logMessage);
  fs.appendFileSync(LOG_FILE, logMessage);
}

// إنشاء المجلدات المطلوبة
function ensureDirectories() {
  try {
    if (!fs.existsSync(AUTH_DIR)) {
      fs.mkdirSync(AUTH_DIR, { recursive: true });
      log('تم إنشاء مجلد الجلسات');
    }
    
    if (!fs.existsSync(LOG_FILE)) {
      fs.writeFileSync(LOG_FILE, '');
      log('تم إنشاء ملف السجل');
    }
    
    if (!fs.existsSync(SESSION_FILE)) {
      fs.writeFileSync(SESSION_FILE, JSON.stringify({}));
      log('تم إنشاء ملف الجلسات');
    }
  } catch (err) {
    console.error('خطأ في إنشاء المجلدات:', err);
    process.exit(1);
  }
}

// إيقاف العملية التي تستخدم المنفذ
async function killProcessOnPort(port) {
  try {
    log('محاولة إيقاف العملية...');
    
    // البحث عن العمليات التي تستخدم المنفذ
    const { stdout } = await execPromise(`lsof -i :${port} -t`);
    if (!stdout.trim()) {
      log('لا توجد عمليات تستخدم المنفذ');
      return true;
    }

    const pids = stdout.trim().split('\n');
    for (const pid of pids) {
      try {
        // محاولة إنهاء العملية بشكل طبيعي أولاً
        await execPromise(`kill ${pid}`);
        log(`تم إرسال إشارة إنهاء للعملية ${pid}`);
        
        // انتظار ثانية للتأكد من إنهاء العملية
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        // التحقق من وجود العملية
        try {
          await execPromise(`ps -p ${pid}`);
          // إذا وصلنا إلى هنا، العملية لا تزال موجودة
          log(`العملية ${pid} لا تزال موجودة، جاري إنهائها بقوة...`);
          await execPromise(`kill -9 ${pid}`);
          log(`تم إنهاء العملية ${pid} بقوة`);
        } catch (err) {
          // إذا وصلنا إلى هنا، العملية قد تم إنهاؤها
          log(`العملية ${pid} تم إنهاؤها بنجاح`);
        }
      } catch (err) {
        log(`فشل في إنهاء العملية ${pid}: ${err.message}`);
      }
    }
    
    // انتظار ثانية للتأكد من تحرير المنفذ
    await new Promise(resolve => setTimeout(resolve, 1000));
    
    return true;
  } catch (err) {
    log(`فشل في البحث عن العمليات: ${err.message}`);
    return false;
  }
}

app.use(express.json());
const sessions = {};

// تحميل الجلسات المحفوظة
function loadSavedSessions() {
  try {
    if (fs.existsSync(SESSION_FILE)) {
      const savedData = JSON.parse(fs.readFileSync(SESSION_FILE));
      Object.keys(savedData).forEach(id => {
        if (savedData[id].active) {
          createSession(id, true);
        }
      });
      log('تم تحميل الجلسات المحفوظة');
    }
  } catch (err) {
    log(`خطأ في تحميل الجلسات المحفوظة: ${err.message}`);
  }
}

// حفظ حالة الجلسات
function saveSessions() {
  try {
    const sessionsData = {};
    Object.keys(sessions).forEach(id => {
      sessionsData[id] = {
        active: sessions[id].ready,
        lastUpdate: sessions[id].lastUpdate
      };
    });
    fs.writeFileSync(SESSION_FILE, JSON.stringify(sessionsData, null, 2));
  } catch (err) {
    log(`خطأ في حفظ الجلسات: ${err.message}`);
  }
}

// دالة محسنة لإنشاء الجلسات
async function createSession(id, retries = 3) {
  try {
    log(`بدء إنشاء جلسة جديدة: ${id} (المحاولة ${4 - retries}/3)`);

    // التأكد من وجود مجلد الجلسة
    const sessionDir = path.join(AUTH_DIR, id);
    if (!fs.existsSync(sessionDir)) {
      fs.mkdirSync(sessionDir, { recursive: true });
      log(`تم إنشاء مجلد الجلسة: ${sessionDir}`);
    }

    log(`جاري تهيئة عميل WhatsApp...`);
    const client = new Client({
      authStrategy: new LocalAuth({ 
        clientId: id,
        dataPath: sessionDir
      }),
      puppeteer: {
        headless: true,
        args: [
          '--no-sandbox',
          '--disable-setuid-sandbox',
          '--disable-dev-shm-usage',
          '--single-process'
        ],
        executablePath: process.env.CHROMIUM_PATH || undefined,
        ignoreHTTPSErrors: true,
        timeout: 120000,
        defaultViewport: {
          width: 1280,
          height: 720
        }
      },
      webVersionCache: {
        type: 'remote',
        remotePath: 'https://raw.githubusercontent.com/wppconnect-team/wa-version/main/html/2.2412.54.html'
      },
      qrMaxRetries: 5,
      authTimeoutMs: 120000,
      qrQualityOptions: {
        quality: 0.8,
        margin: 4
      },
      restartOnAuthFail: true,
      takeoverOnConflict: true,
      takeoverTimeoutMs: 60000
    });

    log(`تم إنشاء العميل، جاري إعداد معالجات الأحداث...`);

    client.on('qr', async (qr) => {
      log(`تم إنشاء QR code للجلسة: ${id}`);
      try {
        const qrUrl = await qrcode.toDataURL(qr, {
          errorCorrectionLevel: 'H',
          margin: 4,
          scale: 8
        });
        log(`تم تحويل QR code بنجاح للجلسة: ${id}`);
        sessions[id].qr = qrUrl;
        sessions[id].lastUpdate = new Date();
        log(`تم حفظ QR code للجلسة: ${id}`);
      } catch (err) {
        log(`خطأ في تحويل QR: ${err.message}`);
        log(`تفاصيل الخطأ: ${err.stack}`);
      }
    });

    client.on('authenticated', () => {
      log(`تم المصادقة بنجاح للجلسة: ${id}`);
    });

    client.on('ready', () => {
      log(`✅ الجلسة جاهزة: ${id}`);
      sessions[id].ready = true;
      sessions[id].lastUpdate = new Date();
    });

    client.on('disconnected', (reason) => {
      log(`❌ تم قطع الاتصال بالجلسة: ${id}، السبب: ${reason}`);
      sessions[id].ready = false;
      if (reason === 'NAVIGATION_ERROR' || reason === 'CONNECTION_LOST') {
        log(`إعادة تشغيل الجلسة تلقائياً: ${id}`);
        setTimeout(() => createSession(id), 5000);
      }
    });

    client.on('auth_failure', (msg) => {
      log(`❌ فشل المصادقة للجلسة: ${id}، الرسالة: ${msg}`);
      setTimeout(() => createSession(id), 5000);
    });

    client.on('loading_screen', (percent, message) => {
      log(`جاري التحميل: ${percent}% - ${message}`);
    });

    client.on('change_state', (state) => {
      log(`تغيير حالة الجلسة ${id}: ${state}`);
    });

    sessions[id] = { client, qr: null, ready: false, lastUpdate: new Date() };
    
    log(`جاري تهيئة العميل...`);
    try {
      await client.initialize();
      log(`تم تهيئة العميل بنجاح للجلسة: ${id}`);
    } catch (err) {
      log(`❌ فشل في تهيئة العميل للجلسة ${id}: ${err.message}`);
      log(`تفاصيل الخطأ: ${err.stack}`);
      throw err;
    }

  } catch (err) {
    log(`❌ خطأ في إنشاء الجلسة: ${err.message}`);
    log(`تفاصيل الخطأ: ${err.stack}`);
    
    // محاولة تنظيف وإعادة المحاولة
    try {
      if (sessions[id]?.client) {
        await sessions[id].client.destroy();
        log(`تم تنظيف الجلسة: ${id}`);
      }
    } catch (cleanupErr) {
      log(`خطأ في تنظيف الجلسة: ${cleanupErr.message}`);
    }
    
    if (retries > 0) {
      log(`إعادة المحاولة بعد 5 ثواني... (المحاولات المتبقية: ${retries - 1})`);
      setTimeout(() => createSession(id, retries - 1), 5000);
    } else {
      log(`❌ فشل إنشاء الجلسة بعد عدة محاولات: ${id}`);
    }
  }
}

// مسارات API
app.get('/start/:sessionId', async (req, res) => {
  const id = req.params.sessionId;
  log(`طلب بدء جلسة: ${id}`);
  
  if (!sessions[id]) {
    await createSession(id);
    res.json({ 
      status: 'starting',
      message: 'جاري بدء الجلسة...',
      timestamp: new Date()
    });
  } else {
    res.json({
      status: sessions[id].ready ? 'ready' : 'pending',
      qr_available: !!sessions[id].qr,
      last_update: sessions[id].lastUpdate
    });
  }
});

app.get('/qr/:sessionId', (req, res) => {
  const id = req.params.sessionId;
  log(`طلب QR code للجلسة: ${id}`);

  if (!sessions[id]) {
    log(`❌ الجلسة غير موجودة: ${id}`);
    return res.status(404).send(`
      <div style="text-align:center; padding:20px;">
        <h4 style="color:red;">❌ الجلسة غير موجودة</h4>
        <p>جاري محاولة إنشاء الجلسة تلقائياً...</p>
        <script>
          setTimeout(() => window.location.reload(), 3000);
        </script>
      </div>
    `);
  }

  if (sessions[id].qr) {
    log(`✔ إرسال QR code للجلسة: ${id}`);
    return res.send(`
      <div style="text-align:center; padding:20px;">
        <h3 style="margin-bottom:20px;">🔒 امسح رمز QR لربط الواتساب</h3>
        <img src="${sessions[id].qr}" alt="QR Code" style="max-width:300px;"/>
        <p style="margin-top:20px;">تاريخ الإنشاء: ${sessions[id].lastUpdate.toLocaleString()}</p>
        <button onclick="window.location.reload()" style="margin-top:10px; padding:8px 15px; background:#4CAF50; color:white; border:none; border-radius:4px;">
          تحديث الصفحة
        </button>
      </div>
    `);
  }

  log(`⏳ QR code غير جاهز للجلسة: ${id}`);
  res.status(202).send(`
    <div style="text-align:center; padding:20px;">
      <h4 style="color:orange;">⏳ رمز QR غير جاهز بعد</h4>
      <p>حالة الجلسة: ${sessions[id].ready ? 'جاهزة' : 'في انتظار QR'}</p>
      <p>آخر تحديث: ${sessions[id].lastUpdate.toLocaleString()}</p>
      <button onclick="window.location.reload()" style="margin-top:10px; padding:8px 15px; background:#2196F3; color:white; border:none; border-radius:4px;">
        إعادة تحميل
      </button>
    </div>
  `);
});

app.post('/send/:sessionId', async (req, res) => {
  const { numbers, message } = req.body;
  const id = req.params.sessionId;

  log(`طلب إرسال رسالة إلى ${numbers.length} رقم عبر الجلسة: ${id}`);

  if (!sessions[id]) {
    return res.status(404).json({ error: 'الجلسة غير موجودة' });
  }

  if (!sessions[id].ready) {
    return res.status(400).json({ status: 'not_ready' });
  }

  try {
    const results = [];
    for (const number of numbers) {
      try {
        const chatId = number.includes('@c.us') ? number : `${number}@c.us`;
        await sessions[id].client.sendMessage(chatId, message);
        results.push({ number, status: 'sent' });
      } catch (err) {
        results.push({ number, status: 'failed', error: err.message });
      }
    }

    res.json({ 
      status: 'completed',
      results
    });

  } catch (err) {
    log(`❌ فشل الإرسال: ${err.message}`);
    res.status(500).json({ error: err.message });
  }
});

app.get('/status', (req, res) => {
  const status = {
    status: 'running',
    timestamp: new Date(),
    sessions: Object.keys(sessions).map(id => ({
      id,
      ready: sessions[id].ready,
      qr_available: !!sessions[id].qr,
      last_update: sessions[id].lastUpdate
    })),
    memory_usage: process.memoryUsage()
  };
  res.json(status);
});

// تحسين بدء تشغيل الخادم
async function startServer() {
  try {
    // التأكد من وجود المجلدات المطلوبة
    ensureDirectories();
    
    // محاولة إيقاف أي عملية تستخدم المنفذ
    await killProcessOnPort(port);
    
    // انتظار ثانية واحدة للتأكد من تحرير المنفذ
    await new Promise(resolve => setTimeout(resolve, 1000));
    
    // بدء الخادم
    const server = app.listen(port, () => {
      log(`WhatsApp API يعمل على المنفذ ${port}`);
    });

    // تحميل الجلسات المحفوظة
    loadSavedSessions();
    
    // حفظ الجلسات كل 5 دقائق
    const saveInterval = setInterval(saveSessions, 5 * 60 * 1000);
    
    // معالجة إغلاق الخادم بشكل نظيف
    const cleanup = async (signal) => {
      log(`تم استلام إشارة ${signal}، جاري إغلاق الخادم...`);
      
      // إغلاق جميع الجلسات النشطة
      const closePromises = Object.keys(sessions).map(async (id) => {
        if (sessions[id].client) {
          try {
            await sessions[id].client.destroy();
            log(`تم إغلاق جلسة WhatsApp: ${id}`);
          } catch (err) {
            log(`خطأ في إغلاق جلسة WhatsApp ${id}: ${err.message}`);
          }
        }
      });
      
      try {
        await Promise.all(closePromises);
        log('تم إغلاق جميع الجلسات بنجاح');
      } catch (err) {
        log(`خطأ في إغلاق الجلسات: ${err.message}`);
      }
      
      // إغلاق الخادم
      server.close(() => {
        log('تم إغلاق الخادم بنجاح');
        process.exit(0);
      });
    };
    
    // معالجة الإشارات
    process.on('SIGTERM', () => cleanup('SIGTERM'));
    process.on('SIGINT', () => cleanup('SIGINT'));
    
    // معالجة الأخطاء غير المعالجة
    process.on('uncaughtException', (err) => {
      log(`خطأ غير معالج: ${err.message}`);
      log(err.stack);
      cleanup('uncaughtException');
    });
    
    process.on('unhandledRejection', (reason, promise) => {
      log(`وعد مرفوض غير معالج: ${reason}`);
      cleanup('unhandledRejection');
    });
    
    server.on('error', async (err) => {
      if (err.code === 'EADDRINUSE') {
        log(`❌ المنفذ ${port} مستخدم بالفعل. جاري محاولة إيقاف العملية القديمة...`);
        const killed = await killProcessOnPort(port);
        if (killed) {
          setTimeout(() => {
            server.listen(port, () => {
              log(`WhatsApp API يعمل على المنفذ ${port}`);
            });
          }, 1000);
        } else {
          log('❌ فشل في إيقاف العملية القديمة');
          process.exit(1);
        }
      } else {
        log(`❌ خطأ في بدء تشغيل الخادم: ${err.message}`);
        process.exit(1);
      }
    });
    
  } catch (err) {
    log(`❌ خطأ في بدء تشغيل الخادم: ${err.message}`);
    process.exit(1);
  }
}

// بدء تشغيل الخادم
startServer();