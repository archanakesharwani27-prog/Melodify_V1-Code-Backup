'use client';

import React, { Suspense, useMemo, useRef, useState, useEffect } from 'react';
import { Canvas, useFrame } from '@react-three/fiber';
import { Float, MeshDistortMaterial, PerspectiveCamera, Stars } from '@react-three/drei';
import * as THREE from 'three';

function ParticleField() {
  const count = 1000; // Reduced count to save GPU
  const positions = useMemo(() => {
    const pos = new Float32Array(count * 3);
    for (let i = 0; i < count; i++) {
      pos[i * 3] = (Math.random() - 0.5) * 20;
      pos[i * 3 + 1] = (Math.random() - 0.5) * 20;
      pos[i * 3 + 2] = (Math.random() - 0.5) * 20;
    }
    return pos;
  }, []);

  const pointsRef = useRef<THREE.Points>(null!);
  useFrame((state) => {
    if (pointsRef.current) {
        pointsRef.current.rotation.y = state.clock.getElapsedTime() * 0.05;
    }
  });

  return (
    <points ref={pointsRef}>
      <bufferGeometry>
        <bufferAttribute
          attach="attributes-position"
          count={count}
          array={positions}
          itemSize={3}
        />
      </bufferGeometry>
      <pointsMaterial size={0.02} color="#6C5CE7" transparent opacity={0.3} sizeAttenuation={true} />
    </points>
  );
}

function FloatingBlobs() {
  const mesh1 = useRef<THREE.Mesh>(null!);

  useFrame((state) => {
    const t = state.clock.getElapsedTime();
    if (mesh1.current) {
        mesh1.current.position.y = Math.sin(t * 0.5) * 0.5;
        mesh1.current.rotation.x = t * 0.2;
    }
  });

  return (
    <Float speed={2} rotationIntensity={1} floatIntensity={1}>
      <mesh ref={mesh1} position={[0, 0, -2]}>
        <sphereGeometry args={[1.5, 32, 32]} />
        <MeshDistortMaterial color="#6C5CE7" speed={2} distort={0.3} roughness={0.1} metalness={0.8} />
      </mesh>
    </Float>
  );
}

export default function HeroVisual() {
  const [webglError, setWebglError] = useState(false);

  useEffect(() => {
    // Check if WebGL is supported
    try {
      const canvas = document.createElement('canvas');
      const support = !!(window.WebGLRenderingContext && (canvas.getContext('webgl') || canvas.getContext('experimental-webgl')));
      if (!support) setWebglError(true);
    } catch (e) {
      setWebglError(true);
    }
  }, []);

  if (webglError) {
    return <div className="absolute inset-0 bg-gradient-to-br from-[#020202] via-[#0a0a1a] to-[#020202] opacity-50 z-0" />;
  }

  return (
    <div className="absolute inset-0 z-0 opacity-60 pointer-events-none">
      <Canvas
        dpr={[1, 1.5]} // Capped DPR for performance
        gl={{
            antialias: false,
            powerPreference: "high-performance",
            alpha: true
        }}
        onCreated={({ gl }) => {
            gl.setClearColor(new THREE.Color('#020202'), 0);
        }}
        onError={() => setWebglError(true)}
      >
        <PerspectiveCamera makeDefault position={[0, 0, 8]} fov={50} />
        <ambientLight intensity={0.2} />
        <pointLight position={[10, 10, 10]} intensity={1.5} color="#6C5CE7" />
        <Suspense fallback={null}>
          <ParticleField />
          <FloatingBlobs />
          <Stars radius={50} depth={50} count={2000} factor={4} saturation={0} fade speed={1} />
        </Suspense>
      </Canvas>
    </div>
  );
}
