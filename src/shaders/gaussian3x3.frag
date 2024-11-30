#ifdef GL_ES
precision mediump float;
#endif

in vec4 v_color;
in vec2 v_texCoords;
uniform sampler2D u_texture;

// recommended value: n / vec2(fbo_width, fbo_height) where n is something like {0.5, 1, 2, 4, ... }
// that, or simply 0.5, depending on how your uv coord works
uniform vec2 halfpixel = vec2(0.0, 0.0);

const vec2 twister = vec2(1.0, -1.0);
const vec2 boolean = vec2(1.0, 0.0);

out vec4 fragColor;

void main() {
    vec4 sum = texture(u_texture, v_texCoords) * 4.0 + // C
    texture(u_texture, v_texCoords + halfpixel) + // SE
    texture(u_texture, v_texCoords - halfpixel) + // NW
    texture(u_texture, v_texCoords + halfpixel * twister) + // NE
    texture(u_texture, v_texCoords - halfpixel * twister) + // SW
    texture(u_texture, v_texCoords + halfpixel * boolean.xy) * 2.0 + // E
    texture(u_texture, v_texCoords - halfpixel * boolean.xy) * 2.0 + // W
    texture(u_texture, v_texCoords + halfpixel * boolean.yx) * 2.0 + // S
    texture(u_texture, v_texCoords - halfpixel * boolean.yx) * 2.0 ; // N

    fragColor = sum / 16.0;
}