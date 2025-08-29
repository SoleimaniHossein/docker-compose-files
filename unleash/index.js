const { Unleash } = require('unleash-client');

const client = new Unleash({
  url: 'http://localhost:4242/api',
  appName: 'my-node-app',
  customHeaders: {
    Authorization: 'default:development.unleash-insecure-api-token'
  }
});

client.on('ready', () => {
  console.log('✅ Unleash client ready!');
  console.log('Is new-button enabled?', client.isEnabled('new-button'));
});

client.start();

