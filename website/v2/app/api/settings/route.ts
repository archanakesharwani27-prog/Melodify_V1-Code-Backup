import { NextResponse } from 'next/server';
import { cookies } from 'next/headers';
import { createClient } from '@supabase/supabase-js';

const supabase = createClient(
  process.env.NEXT_PUBLIC_SUPABASE_URL!,
  process.env.SUPABASE_SERVICE_ROLE_KEY!
);

async function isAuthenticated() {
  const cookieStore = await cookies();
  const session = cookieStore.get('admin_session');
  if (!session) return false;

  const { data } = await supabase
    .from('site_settings')
    .select('data')
    .eq('id', 1)
    .single();

  return session.value === (data?.data?.adminKey || process.env.ADMIN_PASSWORD);
}

export async function GET() {
  try {
    const { data, error } = await supabase
      .from('site_settings')
      .select('data')
      .eq('id', 1)
      .single();

    if (error || !data) throw new Error('Settings not found');

    // Remove sensitive data
    const { adminKey, ...publicSettings } = data.data;
    return NextResponse.json(publicSettings);
  } catch (error) {
    return NextResponse.json({ error: 'Failed to load settings' }, { status: 500 });
  }
}

export async function POST(request: Request) {
  if (!await isAuthenticated()) {
    return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
  }

  try {
    const newSettings = await request.json();

    // Get current settings first
    const { data: current } = await supabase
      .from('site_settings')
      .select('data')
      .eq('id', 1)
      .single();

    const updatedData = {
      ...(current?.data || {}),
      ...newSettings
    };

    // Keep adminKey if not changing
    if (!newSettings.adminKey || newSettings.adminKey.trim() === '') {
      updatedData.adminKey = current?.data?.adminKey || process.env.ADMIN_PASSWORD;
    }

    const { error } = await supabase
      .from('site_settings')
      .update({ data: updatedData })
      .eq('id', 1);

    if (error) throw error;
    return NextResponse.json({ message: 'Settings updated successfully' });
  } catch (error) {
    return NextResponse.json({ error: 'Failed to save settings' }, { status: 500 });
  }
}
