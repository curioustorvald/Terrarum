precision highp float;

uniform vec3 iResolution;
uniform sampler2D iChannel0;
uniform bool flip;
uniform vec2 direction;

//uniform mat4 u_projTrans;

#pragma glslify: blur = require('13')

void main() {
  vec2 uv = vec2(gl_FragCoord.xy / iResolution.xy);
  if (flip) {
    uv.y = 1.0 - uv.y;
  }

  gl_FragColor = blur(iChannel0, uv, iResolution.xy, direction);
}