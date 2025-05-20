from flask import Flask
from flask_sqlalchemy import SQLAlchemy
from flask_cors import CORS
from flask_jwt_extended import JWTManager
from flask_migrate import Migrate
from config import Config
import logging

db = SQLAlchemy()
jwt = JWTManager()
migrate = Migrate()

def create_app():
    app = Flask(__name__)
    app.config.from_object(Config)
    
    # إعداد التسجيل
    logging.basicConfig(level=logging.INFO)
    
    # تهيئة الإضافات
    db.init_app(app)
    jwt.init_app(app)
    migrate.init_app(app, db)
    CORS(app)
    
    # تسجيل المسارات
    from app.routes import main
    app.register_blueprint(main)
    
    # إنشاء قاعدة البيانات وتطبيق الترحيلات تلقائياً
    with app.app_context():
        db.create_all()
        try:
            # محاولة تنفيذ الترحيلات تلقائياً
            from flask_migrate import upgrade
            upgrade()
        except Exception as e:
            logging.error(f"Error during automatic migration: {str(e)}")
    
    return app 