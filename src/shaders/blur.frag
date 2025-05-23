#ifdef GL_ES
    precision mediump float;
#endif


in vec4 v_color;
in vec2 v_texCoords;

uniform sampler2D u_texture;

uniform vec2 iResolution;
uniform float flip;
uniform vec2 direction;
out vec4 fragColor;

vec4 blur(sampler2D image, vec2 uv, vec2 resolution, vec2 direction) {
    vec4 color = vec4(0.0);
    vec2 off1 = vec2(1.3846153846) * direction;
    vec2 off2 = vec2(3.2307692308) * direction;
    color += texture(image, uv) * 0.2270270270;
    color += texture(image, uv + (off1 / resolution)) * 0.3162162162;
    color += texture(image, uv - (off1 / resolution)) * 0.3162162162;
    color += texture(image, uv + (off2 / resolution)) * 0.0702702703;
    color += texture(image, uv - (off2 / resolution)) * 0.0702702703;
    return color;
}

void main() {
  vec2 uv = vec2(gl_FragCoord.xy / iResolution.xy);
  if (flip == 1.0) {
    uv.y = 1.0 - uv.y;
  }

  fragColor = blur(u_texture, uv, iResolution.xy, direction);
}