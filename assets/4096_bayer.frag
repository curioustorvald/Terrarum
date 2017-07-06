varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;



uniform mat4 Bayer;
uniform float monitorGamma;

vec4 gammaIn(vec4 col) {
    return pow(col, vec4(vec3(monitorGamma), 1.0));
}

vec4 gammaOut(vec4 col) {
    return pow(col, vec4(vec3(1.0 / monitorGamma), 1.0));
}

void main(void) {
    // create texture coordinates based on pixelSize //
    float pixelSize = 1.0;

    vec2 pixelSizeVec = vec2(float(pixelSize), float(pixelSize));

    vec2 discrete = (gl_FragCoord.xy + 0.001) / v_texCoords / pixelSizeVec;
    //vec2 discrete = (gl_FragCoord.xy) / v_texCoords / pixelSizeVec;

    discrete = floor(discrete * v_texCoords) / discrete;

    vec4 color = texture2D(u_texture, discrete).rgba;


    // add Bayer matrix entry to current pixel //
    vec2 entry = mod(gl_FragCoord.xy / pixelSizeVec, vec2(4, 4));

    color.r = color.r + Bayer[int(entry.x)][int(entry.y)] / 17.0 - 0.5;
    color.g = color.g + Bayer[int(entry.x)][int(entry.y)] / 17.0 - 0.5;
    color.b = color.b + Bayer[int(entry.x)][int(entry.y)] / 17.0 - 0.5;
    //color.a = color.a + Bayer[int(entry.x)][int(entry.y)] / 17.0 - 0.5;


    // find nearest 8-bit color //
    color.r = floor(15.0 * color.r + 0.5) / 15.0;
    color.g = floor(15.0 * color.g + 0.5) / 15.0;
    color.b = floor(15.0 * color.b + 0.5) / 15.0;
    //color.a = floor(15.0 * color.a + 0.5) / 15.0;

    gl_FragColor = color;







    //vec4 color = texture2D(u_texture, v_texCoords);
    //color = floor(15.0 * color + 0.5) / 15.0;
//
    //gl_FragColor = color;
}