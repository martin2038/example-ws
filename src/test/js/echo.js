import WebSocket from 'ws';

const ws = new WebSocket('ws://127.0.0.1:8083/path-ignored', {
  perMessageDeflate: false,
});

ws.on('error', console.error);

ws.on('open', function open() {
  console.log('ws success opened!');
  ws.send('Hello!');
});

ws.on('message', function message(data) {
  console.log('received: %s', data.length > 1000 ? data.length + '.....' : data);
});

let i = 0;
const lens = [100, 200, 500, 10000, 50000, 80000];
setInterval(() => ws.send(ranMsg(lens[i++ % lens.length])), 2000);

const ten = 'hello12345';

const ranMsg = (len) => {//: number
  console.log('gen msg with len', len);
  let msg = '';
  for (let i = 0; i < len / 10; i++) {
    msg += ten;
  }
  return msg;
};
