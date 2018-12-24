'use strict';

window.addEventListener('message', function (e) {
  if (e.origin !== window.location.origin) {
    return;
  }
  if (e.data.to !== 'content') {
    return;
  }
  if (e.data.type === 'webrtc-reload') {
    window.location.reload();
  }
});

(function modifyWebRTC(apis) {
  apis.forEach(function (api) {
    if (!(api && api.prototype.createOffer)) {
      return;
    }
    var createOffer = api.prototype.createOffer;
    api.prototype.createOffer = function () {
      window.postMessage({
        type: 'webrtc-call',
        to: 'background'
      }, window.location.origin);
      return createOffer.apply(this, [].slice.call(arguments));
    };
  });
})([window.RTCPeerConnection, window.webkitRTCPeerConnection, window.mozRTCPeerConnection, window.msRTCPeerConnection]);