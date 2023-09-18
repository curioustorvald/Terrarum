#ifdef GL_ES
    precision mediump float;
#endif

in vec4 v_color;
in vec2 v_texCoords;
uniform sampler2D u_texture;

// recommended value: n / vec2(fbo_width, fbo_height) where n is something like {0.5, 1, 2, 4, ... }
// that, or simply 0.5, depending on how your uv coord works
uniform vec2 halfpixel = vec2(0.0, 0.0);

const vec2 doublex = vec2(2.0, 0.0);
const vec2 doubley = vec2(0.0, 2.0);
const vec2 twister = vec2(1.0, -1.0);
const vec2 boolean = vec2(1.0, 0.0);

out vec4 fragColor;

void main() {
    vec4 sum = texture(u_texture, v_texCoords) +
    texture(u_texture, v_texCoords + halfpixel) +
    texture(u_texture, v_texCoords - halfpixel) +
    texture(u_texture, v_texCoords + halfpixel * twister) +
    texture(u_texture, v_texCoords - halfpixel * twister) +
    texture(u_texture, v_texCoords + halfpixel * boolean.xy) +
    texture(u_texture, v_texCoords - halfpixel * boolean.xy) +
    texture(u_texture, v_texCoords + halfpixel * boolean.yx) +
    texture(u_texture, v_texCoords - halfpixel * boolean.yx) ;

    fragColor = sum / 9.0;
}