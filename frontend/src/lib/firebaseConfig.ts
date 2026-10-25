/** Cloudinary settings are public in browser builds; use a restricted unsigned preset. */
export const CLOUDINARY_CLOUD_NAME = import.meta.env.VITE_CLOUDINARY_CLOUD_NAME || '';
export const CLOUDINARY_UPLOAD_PRESET = import.meta.env.VITE_CLOUDINARY_UPLOAD_PRESET || '';
export const CLOUDINARY_REPAIR_FOLDER = 'fixlink/repair-requests';
export const CLOUDINARY_KYC_FOLDER = 'fixlink/kyc-documents';
