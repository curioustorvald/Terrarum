#version 120
#ifdef GL_ES
    precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;

// recommended value: n / vec2(fbo_width, fbo_height) where n is something like {0.5, 1, 2, 4, ... }
// that, or simply 0.5, depending on how your uv coord works
uniform vec2 halfpixel = vec2(0.0, 0.0);

vec2 doublex = vec2(2.0, 0.0);
vec2 doubley = vec2(0.0, 2.0);
vec2 twister = vec2(1.0, -1.0);

void main() {
    vec4 sum = texture2D(u_texture, v_texCoords - halfpixel * doublex);
    sum += texture2D(u_texture, v_texCoords - halfpixel * twister) * 2.0;
    sum += texture2D(u_texture, v_texCoords + halfpixel * doubley);
    sum += texture2D(u_texture, v_texCoords + halfpixel) * 2.0;
    sum += texture2D(u_texture, v_texCoords + halfpixel * doublex);
    sum += texture2D(u_texture, v_texCoords + halfpixel * twister) * 2.0;
    sum += texture2D(u_texture, v_texCoords - halfpixel * doubley);
    sum += texture2D(u_texture, v_texCoords - halfpixel) * 2.0;
    gl_FragColor = sum / 12.0;

//    gl_FragColor = texture2D(u_texture, v_texCoords);
}