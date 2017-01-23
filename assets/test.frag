#ifdef GL_ES
    precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform mat4 u_projTrans;

void main() {
        vec3 color = texture2D(u_texture, v_texCoords).rgb;
        float gray = (3.0 * color.r + 4.0 * color.g + color.b) / 8.0;
        vec3 grayscale = vec3(gray);

        gl_FragColor = vec4(grayscale, 1.0);
}