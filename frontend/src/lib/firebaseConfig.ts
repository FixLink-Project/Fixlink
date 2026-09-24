/**
 * FixLink - Cấu hình Firebase Storage (Firebase Web SDK v10 Modular).
 * Hỗ trợ các biến môi trường VITE_FIREBASE_* nếu được cung cấp.
 */
export const FIREBASE_CONFIG = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY || '',
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN || '',
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID || '',
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET || '',
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID || '',
  appId: import.meta.env.VITE_FIREBASE_APP_ID || ''
};

export const FIREBASE_STORAGE_FOLDER = 'fixlink/repair-requests';
export const FIREBASE_KYC_FOLDER = 'fixlink/kyc-documents';
