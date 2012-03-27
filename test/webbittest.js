ws = new MozWebSocket('ws://localhost:8072/chatsocket');

ws.onopen = console.log;
ws.onclose = console.log;
ws.onerror = console.log;
ws.onmessage = function(e){console.log(JSON.parse(e.data));};

function send(outgoing) { ws.send(JSON.stringify(outgoing));}
