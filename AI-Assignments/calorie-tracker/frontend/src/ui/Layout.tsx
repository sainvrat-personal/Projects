import React from 'react';

interface LayoutProps {
  children: React.ReactNode;
}

const Layout: React.FC<LayoutProps> = ({ children }) => (
  <main style={{ maxWidth: 1280, margin: '0 auto', width: '100%', padding: '0 1.5rem' }}>
    {children}
  </main>
);

export default Layout;
