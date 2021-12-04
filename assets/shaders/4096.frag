varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;

void main(void) {
    vec4 color = texture2D(u_texture, v_texCoords).rgba;

    color.r = floor(15.0 * color.r + 0.5) / 15.0;
    color.g = floor(15.0 * color.g + 0.5) / 15.0;
    color.b = floor(15.0 * color.b + 0.5) / 15.0;
    // a: passthrough

    gl_FragColor = color;
}