#version 150
#ifdef GL_ES
    precision mediump float;
#endif


in vec4 v_color;
in vec2 v_texCoords;
uniform sampler2D u_texture; // world texture, has alpha value that is meaningful

uniform sampler2D tex1; // glow texture, SHOULD contain alpha of all 1.0
out vec4 fragColor;

const vec2 boolean = vec2(0.0, 1.0);

uniform vec4 drawOffsetSize; // (gradX, gradY, gradW, gradH)
uniform vec4 skyboxUV1; // (u, v, u2, v2) for the skybox drawing (morning)
uniform vec4 skyboxUV2; // (u, v, u2, v2) for the skybox drawing (afternoon)
uniform float texBlend;
uniform vec2 tex1Size = vec2(4096.0);
uniform vec2 astrumScroll = vec2(0.0);
uniform vec4 randomNumber = vec4(1.0, -2.0, 3.0, -4.0);

vec3 mod289(vec3 x) {
    return x - floor(x * (1.0 / 289.0)) * 289.0;
}

vec2 mod289(vec2 x) {
    return x - floor(x * (1.0 / 289.0)) * 289.0;
}

vec3 permute(vec3 x) {
    return mod289(((x*34.0)+10.0)*x);
}

float snoise(vec2 v)
{
    const vec4 C = vec4(0.211324865405187,  // (3.0-sqrt(3.0))/6.0
    0.366025403784439,  // 0.5*(sqrt(3.0)-1.0)
    -0.577350269189626,  // -1.0 + 2.0 * C.x
    0.024390243902439); // 1.0 / 41.0
    // First corner
    vec2 i  = floor(v + dot(v, C.yy) );
    vec2 x0 = v -   i + dot(i, C.xx);

    // Other corners
    vec2 i1;
    //i1.x = step( x0.y, x0.x ); // x0.x > x0.y ? 1.0 : 0.0
    //i1.y = 1.0 - i1.x;
    i1 = (x0.x > x0.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0);
    // x0 = x0 - 0.0 + 0.0 * C.xx ;
    // x1 = x0 - i1 + 1.0 * C.xx ;
    // x2 = x0 - 1.0 + 2.0 * C.xx ;
    vec4 x12 = x0.xyxy + C.xxzz;
    x12.xy -= i1;

    // Permutations
    i = mod289(i); // Avoid truncation effects in permutation
    vec3 p = permute( permute( i.y + vec3(0.0, i1.y, 1.0 ))
    + i.x + vec3(0.0, i1.x, 1.0 ));

    vec3 m = max(0.5 - vec3(dot(x0,x0), dot(x12.xy,x12.xy), dot(x12.zw,x12.zw)), 0.0);
    m = m*m ;
    m = m*m ;

    // Gradients: 41 points uniformly over a line, mapped onto a diamond.
    // The ring size 17*17 = 289 is close to a multiple of 41 (41*7 = 287)

    vec3 x = 2.0 * fract(p * C.www) - 1.0;
    vec3 h = abs(x) - 0.5;
    vec3 ox = floor(x + 0.5);
    vec3 a0 = x - ox;

    // Normalise gradients implicitly by scaling m
    // Approximation of: m *= inversesqrt( a0*a0 + h*h );
    m *= 1.79284291400159 - 0.85373472095314 * ( a0*a0 + h*h );

    // Compute final noise value at P
    vec3 g;
    g.x  = a0.x  * x0.x  + h.x  * x0.y;
    g.yz = a0.yz * x12.xz + h.yz * x12.yw;
    return (65.0 * dot(m, g)) + 0.5;
}

vec4 snoise4(vec2 v) {
    /*return vec4(
        (snoise(v + randomNumber.xy) + snoise(v + randomNumber.zx)) * 0.5,
        (snoise(v + randomNumber.zw) + snoise(v + randomNumber.yw)) * 0.5,
        (snoise(v + randomNumber.xz) + snoise(v + randomNumber.yx)) * 0.5,
        (snoise(v + randomNumber.yw) + snoise(v + randomNumber.wz)) * 0.5 // triangular distribution
    );*/
    return vec4(
        (snoise(v + randomNumber.xy) + snoise(v + randomNumber.zx) + snoise(v + randomNumber.zw) + snoise(v + randomNumber.yw)) * 0.25,
        (snoise(v + randomNumber.zw) + snoise(v + randomNumber.yw) + snoise(v + randomNumber.xz) + snoise(v + randomNumber.yx)) * 0.25,
        (snoise(v + randomNumber.xz) + snoise(v + randomNumber.yx) + snoise(v + randomNumber.yw) + snoise(v + randomNumber.wz)) * 0.25,
        (snoise(v + randomNumber.yw) + snoise(v + randomNumber.wz) + snoise(v + randomNumber.xy) + snoise(v + randomNumber.zx)) * 0.25
    );
}

vec4 random(vec2 p) {
    vec2 K1 = vec2(
        23.14069263277926, // e^pi (Gelfond's constant)
        2.665144142690225 // 2^sqrt(2) (Gelfond-Schneider constant)
    );
    return vec4(
        fract(cos(dot(p + randomNumber.xy, K1)) * 12345.6789),
        fract(cos(dot(p + randomNumber.zw, K1)) * 12345.6789),
        fract(cos(dot(p + randomNumber.xz, K1)) * 12345.6789),
        fract(cos(dot(p + randomNumber.yw, K1)) * 12345.6789)
    );
} // TODO the "grain" needs to be larger

// draw call to this function must use UV coord of (0,0,1,1)!
void main(void) {
    vec2 skyboxTexCoordMorning = mix(skyboxUV1.xy, skyboxUV1.zw, v_texCoords);
    vec2 skyboxTexCoordAfternoon = mix(skyboxUV2.xy, skyboxUV2.zw, v_texCoords);

    vec2 astrumTexCoord = (v_texCoords * drawOffsetSize.zw + drawOffsetSize.xy + astrumScroll) / tex1Size;

    vec4 randomness = snoise4((gl_FragCoord.xy - astrumScroll) * 0.16) * 2.0; // multiply by 2 so that the "density" of the stars would be same as the non-random version


    vec4 colorTex0Morining = texture(u_texture, skyboxTexCoordMorning);
    vec4 colorTex0Afternoon = texture(u_texture, skyboxTexCoordAfternoon);
    vec4 colorTex0 = mix(colorTex0Morining, colorTex0Afternoon, texBlend);
    vec4 colorTex1 = texture(tex1, astrumTexCoord) * randomness;



    fragColor = (max(colorTex0, colorTex1) * boolean.yyyx) + boolean.xxxy;
//    fragColor = colorTex1;
//    fragColor = randomness * boolean.yyyx + boolean.xxxy;
//    fragColor = (randomness.rrrr + (colorTex1 * vec4(2.0, -2.0, 2.0, 1.0))) * boolean.yyyx + boolean.xxxy;
}