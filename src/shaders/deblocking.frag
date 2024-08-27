#ifdef GL_ES
precision mediump float;
#endif


in vec4 v_color;
in vec2 v_texCoords;

out vec4 fragColor;

uniform sampler2D u_texture;
uniform sampler2D u_blurmap;
uniform vec2 resolution;

const vec4 _four = vec4(1.0 / 4.0);
const vec4 _two = vec4(1.0 / 2.0);
const float blur = 1.0;
vec2 blurUp = vec2(0.0, -blur);
vec2 blurDown = vec2(0.0, +blur);
vec2 blurLeft = vec2(-blur, 0.0);
vec2 blurRight = vec2(+blur, 0.0);

void main(void) {
    vec4 rgbColourIn = texture(u_texture, v_texCoords);
    vec4 rgbColourL = texture(u_texture, v_texCoords + (blurLeft / resolution));
    vec4 rgbColourR = texture(u_texture, v_texCoords + (blurRight / resolution));
    vec4 rgbColourU = texture(u_texture, v_texCoords + (blurUp / resolution));
    vec4 rgbColourD = texture(u_texture, v_texCoords + (blurDown / resolution));

    vec4 blurH = (rgbColourIn + rgbColourIn + rgbColourL + rgbColourR) * _four;
    vec4 blurV = (rgbColourIn + rgbColourIn + rgbColourU + rgbColourD) * _four;

    vec4 mapCol = texture(u_blurmap, v_texCoords);

    fragColor = v_color * mix(
        mix(rgbColourIn, blurH, mapCol.x),
        mix(rgbColourIn, blurV, mapCol.y),
        0.5
    );

    fragColor = rgbColourIn;
//    fragColor = vec4(v_texCoords.x, v_texCoords.y, 0.0, 1.0);
}