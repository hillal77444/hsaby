from app import create_app
import subprocess
import os
import signal
import sys
import time

def start_whatsapp_server():
    try:
        # تشغيل خادم WhatsApp في الخلفية
        whatsapp_process = subprocess.Popen(
            ['node', 'whatsapp_server.js'],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            preexec_fn=os.setsid
        )
        print("✅ تم بدء تشغيل خادم WhatsApp")
        return whatsapp_process
    except Exception as e:
        print(f"❌ خطأ في تشغيل خادم WhatsApp: {str(e)}")
        return None

def cleanup(whatsapp_process):
    if whatsapp_process:
        try:
            # إيقاف خادم WhatsApp
            os.killpg(os.getpgid(whatsapp_process.pid), signal.SIGTERM)
            print("✅ تم إيقاف خادم WhatsApp")
        except Exception as e:
            print(f"❌ خطأ في إيقاف خادم WhatsApp: {str(e)}")

def main():
    app = create_app()
    whatsapp_process = start_whatsapp_server()

    try:
        # تشغيل الخادم الرئيسي
        app.run(debug=False, host='0.0.0.0', port=5007)
    except KeyboardInterrupt:
        print("\n🛑 جاري إيقاف الخوادم...")
    finally:
        cleanup(whatsapp_process)

if __name__ == '__main__':
    main() 