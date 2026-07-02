import { NextResponse } from 'next/server';
import { cookies } from 'next/headers';
import admin from 'firebase-admin';

if (!admin.apps.length) {
  admin.initializeApp({
    credential: admin.credential.cert({
      projectId: process.env.FIREBASE_PROJECT_ID,
      clientEmail: process.env.FIREBASE_CLIENT_EMAIL,
      privateKey: process.env.FIREBASE_PRIVATE_KEY?.replace(/\\n/g, '\n'),
    }),
    storageBucket: process.env.FIREBASE_STORAGE_BUCKET,
  });
}

const bucket = admin.storage().bucket();

export async function POST(request: Request) {
  // Simple session check via cookie presence (validation happens in settings)
  const cookieStore = await cookies();
  if (!cookieStore.get('admin_session')) {
    return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
  }

  try {
    const formData = await request.formData();
    const file = formData.get('file') as File;

    if (!file) return NextResponse.json({ error: 'No file' }, status: 400);

    const allowedTypes = ['image/jpeg', 'image/png', 'image/webp'];
    if (!allowedTypes.includes(file.type)) {
      return NextResponse.json({ error: 'Only images allowed' }, { status: 400 });
    }

    const buffer = Buffer.from(await file.arrayBuffer());
    const filename = `melodify/${Date.now()}-${file.name}`;
    const fileUpload = bucket.file(filename);

    await fileUpload.save(buffer, {
      metadata: { contentType: file.type },
    });

    // Make public and get URL
    await fileUpload.makePublic();
    const publicUrl = `https://storage.googleapis.com/${bucket.name}/${filename}`;

    return NextResponse.json({ url: publicUrl });
  } catch (error) {
    console.error('Firebase Error:', error);
    return NextResponse.json({ error: 'Upload failed' }, { status: 500 });
  }
}
