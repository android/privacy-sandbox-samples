function encodeSignals(signals, maxSize) {
// if there are no signals don't write a payload
if (signals.size === 0) {
return {};
}

let result = new Uint8Array(signals.size * 2);
let index = 0;

for (const [key, values] of signals.entries()) {
result[index++] = key[0];
result[index++] = values[0].signal_value[0];
}

return { 'status': 0, 'results': result};
}