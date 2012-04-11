// manual setup
if(typeof WebSocket === 'undefined'){
    WebSocket = MozWebSocket;
}

ws = new WebSocket('ws://localhost:9876/chatsocket');

function send(outgoing) { ws.send(JSON.stringify(outgoing));}

// console.log only for demo purposes
ws.onopen = console.log;  
ws.onclose = console.log; 
ws.onerror = console.log; 
ws.onmessage = function(e){console.log(JSON.parse(e.data));};


////// examples

// logging in
send({action:'LOGIN', loginUsername: "My Name"});


// exercising the userlist function
send({action: "USERLIST"});

// sending stuff
send({action: "SAY", message: "This is a test message"});

// clients can send any json-ish data not just messages
send({action: "SPRAY", data: {foo: "bar", baz: 1, y:2}});

