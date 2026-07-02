import { NextResponse } from 'next/server';
import { cookies } from 'next/headers';
import { createClient } from '@supabase/supabase-js';

const supabase = createClient(
  process.env.NEXT_PUBLIC_SUPABASE_URL!,
  process.env.SUPABASE_SERVICE_ROLE_KEY!
);

export async function POST(request: Request) {
  try {
    const { key } = await request.json();

    const { data } = await supabase
      .from('site_settings')
      .select('data')
      .eq('id', 1)
      .single();

    const adminKey = data?.data?.adminKey || process.env.ADMIN_PASSWORD || 'admin';

    if (key === adminKey) {
      const cookieStore = await cookies();
      cookieStore.set('admin_session', key, {
        httpOnly: true,
        secure: process.env.NODE_ENV === 'production',
        sameSite: 'strict',
        maxAge: 60 * 60 * 24,
        path: '/',
      });
      return NextResponse.json({ success: true });
    } else {
      return NextResponse.json({ success: false }, { status: 401 });
    }
  } catch (error) {
    return NextResponse.json({ error: 'Login failed' }, { status: 500 });
  }
}
