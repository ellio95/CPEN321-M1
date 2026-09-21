import express, { type Express } from 'express';
const axios = require('axios');
import { verifyToken } from './verifyToken'

export function createApp(): Express {
  const app = express();

  app.use(express.json());

  app.get('/health', (_req, res) => {
    res.json({status: "ok"})
  });

  // app.get('/info', (_req, res) => {
  //   const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone;

  //   res.json({ 
  //     timezone: timezone,
  //     author: "Elliot Shepherd",
  //     status: "ok"
  //   });
  // });

  app.get('/ip', async (_req, res) => {
    try {
      const response = await axios.get('https://api.ipify.org?format=json');
      res.json({ip: response.data.ip})
    } catch (error) {
      res.status(500).json({error: "Failed to generate public IP"})
    }
  })

  app.get('/author', (_req, res) => {
    res.json({author: "Elliot Shepherd"})
  })

  app.get('/time', (_req, res) => {
    const now = new Date();

    const time = now.toTimeString().split(' ')[0]; // hh:mm:ss
    const offsetMinutes = -now.getTimezoneOffset();
    const sign = offsetMinutes >= 0 ? '+' : '-';
    const hours = Math.floor(Math.abs(offsetMinutes) / 60).toString().padStart(2, '0');
    const minutes = (Math.abs(offsetMinutes) % 60).toString().padStart(2, '0');

    res.json({ time: `${time} GMT${sign}${hours}:${minutes}` });
  })

  app.post('/login', async (_req, res) => {

    const { idToken } = _req.body

    const payload = await verifyToken(idToken)

    if (payload == null) {
      res.status(500).json({error: "Invalid Token"});
      return;
    }

    res.json({ 
      clientIP: _req.ip,
        userFirstName: payload.given_name,
        userLastName: payload.family_name ?? "",
    });
    
  });

  app.use((_req, res) => {
    res.status(404).json({ error: 'Not Found' });
  });

  return app;
}
