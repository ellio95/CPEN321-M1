import express, { type Express } from 'express';

export function createApp(): Express {
  const app = express();

  app.get('/health', (_req, res) => {
    res.json({ status: 'ok' });
  });

  app.post('/login', (_req, res) => {
    res.json({ status: 'login received' });
    console.log('login received');
  });

  app.use((_req, res) => {
    res.status(404).json({ error: 'Not Found' });
  });

  return app;
}
