varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;

vec2 boolean = vec2(0.0, 1.0);
vec4 desaturate = vec4(0.2126, 0.7152, 0.0722, 0.0);

void main(void) {
    vec4 incolour = texture2D(u_texture, v_texCoords);
    float lum = dot(incolour * desaturate, boolean.yyyx) * 0.5 + 0.5;

    gl_FragColor = v_color * (vec4(lum) * boolean.yyyx + incolour * boolean.xxxy);
}