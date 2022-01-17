/**
 * Copyright (c) 2016 Alexei Boronine
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.torvald.colourutil;

import com.jme3.math.FastMath;

import java.util.ArrayList;
import java.util.List;

public class HUSLColorConverter {
    private static float[][] m = new float[][]
            {
                    new float[]{3.240969941904521f, -1.537383177570093f, -0.498610760293f},
                    new float[]{-0.96924363628087f, 1.87596750150772f, 0.041555057407175f},
                    new float[]{0.055630079696993f, -0.20397695888897f, 1.056971514242878f},
            };

    private static float[][] minv = new float[][]
            {
                    new float[]{0.41239079926595f, 0.35758433938387f, 0.18048078840183f},
                    new float[]{0.21263900587151f, 0.71516867876775f, 0.072192315360733f},
                    new float[]{0.019330818715591f, 0.11919477979462f, 0.95053215224966f},
            };

    private static float refY = 1.0f;

    private static float refU = 0.19783000664283f;
    private static float refV = 0.46831999493879f;

    private static float kappa = 903.2962962f;
    private static float epsilon = 0.0088564516f;

    private static List<float[]> getBounds(float L) {
        ArrayList<float[]> result = new ArrayList<float[]>();

        float sub1 = FastMath.pow(L + 16, 3) / 1560896;
        float sub2 = sub1 > epsilon ? sub1 : L / kappa;

        for (int c = 0; c < 3; ++c) {
            float m1 = m[c][0];
            float m2 = m[c][1];
            float m3 = m[c][2];

            for (int t = 0; t < 2; ++t) {
                float top1 = (284517 * m1 - 94839 * m3) * sub2;
                float top2 = (838422 * m3 + 769860 * m2 + 731718 * m1) * L * sub2 - 769860 * t * L;
                float bottom = (632260 * m3 - 126452 * m2) * sub2 + 126452 * t;

                result.add(new float[]{top1 / bottom, top2 / bottom});
            }
        }

        return result;
    }

    private static float intersectLineLine(float[] lineA, float[] lineB) {
        return (lineA[1] - lineB[1]) / (lineB[0] - lineA[0]);
    }

    private static float distanceFromPole(float[] point) {
        return FastMath.sqrt(FastMath.pow(point[0], 2) + FastMath.pow(point[1], 2));
    }

    private static Length lengthOfRayUntilIntersect(float theta, float[] line) {
        float length = line[1] / (FastMath.sin(theta) - line[0] * FastMath.cos(theta));

        return new Length(length);
    }

    private static class Length {
        final boolean greaterEqualZero;
        final float length;


        private Length(float length) {
            this.greaterEqualZero = length >= 0;
            this.length = length;
        }
    }

    private static float maxSafeChromaForL(float L) {
        List<float[]> bounds = getBounds(L);
        float min = Float.MAX_VALUE;

        for (int i = 0; i < 2; ++i) {
            float m1 = bounds.get(i)[0];
            float b1 = bounds.get(i)[1];
            float[] line = new float[]{m1, b1};

            float x = intersectLineLine(line, new float[]{-1 / m1, 0});
            float length = distanceFromPole(new float[]{x, b1 + x * m1});

            min = FastMath.min(min, length);
        }

        return min;
    }

    private static float maxChromaForLH(float L, float H) {
        float hrad = H / 360 * FastMath.PI * 2;

        List<float[]> bounds = getBounds(L);
        float min = Float.MAX_VALUE;

        for (float[] bound : bounds) {
            Length length = lengthOfRayUntilIntersect(hrad, bound);
            if (length.greaterEqualZero) {
                min = FastMath.min(min, length.length);
            }
        }

        return min;
    }

    private static float dotProduct(float[] a, float[] b) {
        float sum = 0;

        for (int i = 0; i < a.length; ++i) {
            sum += a[i] * b[i];
        }

        return sum;
    }

    private static float round(float value, int places) {
        float n = FastMath.pow(10, places);

        return Math.round(value * n) / n;
    }

    private static float fromLinear(float c) {
        if (c <= 0.0031308) {
            return 12.92f * c;
        } else {
            return 1.055f * FastMath.pow(c, 1 / 2.4f) - 0.055f;
        }
    }

    private static float toLinear(float c) {
        if (c > 0.04045) {
            return FastMath.pow((c + 0.055f) / (1 + 0.055f), 2.4f);
        } else {
            return c / 12.92f;
        }
    }

    private static int[] rgbPrepare(float[] tuple) {

        int[] results = new int[tuple.length];

        for (int i = 0; i < tuple.length; ++i) {
            float chan = tuple[i];
            float rounded = round(chan, 3);

            if (rounded < -0.0001 || rounded > 1.0001) {
                throw new IllegalArgumentException("Illegal rgb value: " + rounded);
            }

            results[i] = (int) Math.round(rounded * 255);
        }

        return results;
    }

    public static float[] xyzToRgb(float[] tuple) {
        return new float[]
                {
                        fromLinear(dotProduct(m[0], tuple)),
                        fromLinear(dotProduct(m[1], tuple)),
                        fromLinear(dotProduct(m[2], tuple)),
                };
    }

    public static float[] rgbToXyz(float[] tuple) {
        float[] rgbl = new float[]
                {
                        toLinear(tuple[0]),
                        toLinear(tuple[1]),
                        toLinear(tuple[2]),
                };

        return new float[]
                {
                        dotProduct(minv[0], rgbl),
                        dotProduct(minv[1], rgbl),
                        dotProduct(minv[2], rgbl),
                };
    }

    private static float yToL(float Y) {
        if (Y <= epsilon) {
            return (Y / refY) * kappa;
        } else {
            return 116 * FastMath.pow(Y / refY, 1.0f / 3.0f) - 16;
        }
    }

    private static float lToY(float L) {
        if (L <= 8) {
            return refY * L / kappa;
        } else {
            return refY * FastMath.pow((L + 16) / 116, 3);
        }
    }

    public static float[] xyzToLuv(float[] tuple) {
        float X = tuple[0];
        float Y = tuple[1];
        float Z = tuple[2];

        float varU = (4 * X) / (X + (15 * Y) + (3 * Z));
        float varV = (9 * Y) / (X + (15 * Y) + (3 * Z));

        float L = yToL(Y);

        if (L == 0) {
            return new float[]{0, 0, 0};
        }

        float U = 13 * L * (varU - refU);
        float V = 13 * L * (varV - refV);

        return new float[]{L, U, V};
    }

    public static float[] luvToXyz(float[] tuple) {
        float L = tuple[0];
        float U = tuple[1];
        float V = tuple[2];

        if (L == 0) {
            return new float[]{0, 0, 0};
        }

        float varU = U / (13 * L) + refU;
        float varV = V / (13 * L) + refV;

        float Y = lToY(L);
        float X = 0 - (9 * Y * varU) / ((varU - 4) * varV - varU * varV);
        float Z = (9 * Y - (15 * varV * Y) - (varV * X)) / (3 * varV);

        return new float[]{X, Y, Z};
    }

    public static float[] luvToLch(float[] tuple) {
        float L = tuple[0];
        float U = tuple[1];
        float V = tuple[2];

        float C = FastMath.sqrt(U * U + V * V);
        float H;

        if (C < 0.00000001) {
            H = 0;
        } else {
            float Hrad = FastMath.atan2(V, U);

            // pi to more digits than they provide it in the stdlib
            H = (Hrad * 180.0f) / 3.1415926535897932f;

            if (H < 0) {
                H = 360 + H;
            }
        }

        return new float[]{L, C, H};
    }

    public static float[] lchToLuv(float[] tuple) {
        float L = tuple[0];
        float C = tuple[1];
        float H = tuple[2];

        float Hrad = H / 360.0f * 2 * FastMath.PI;
        float U = FastMath.cos(Hrad) * C;
        float V = FastMath.sin(Hrad) * C;

        return new float[]{L, U, V};
    }

    public static float[] hsluvToLch(float[] tuple) {
        float H = tuple[0];
        float S = tuple[1];
        float L = tuple[2];

        if (L > 99.9999999) {
            return new float[]{100f, 0, H};
        }

        if (L < 0.00000001) {
            return new float[]{0, 0, H};
        }

        float max = maxChromaForLH(L, H);
        float C = max / 100 * S;

        return new float[]{L, C, H};
    }

    public static float[] lchToHsluv(float[] tuple) {
        float L = tuple[0];
        float C = tuple[1];
        float H = tuple[2];

        if (L > 99.9999999) {
            return new float[]{H, 0, 100};
        }

        if (L < 0.00000001) {
            return new float[]{H, 0, 0};
        }

        float max = maxChromaForLH(L, H);
        float S = C / max * 100;

        return new float[]{H, S, L};
    }

    public static float[] hpluvToLch(float[] tuple) {
        float H = tuple[0];
        float S = tuple[1];
        float L = tuple[2];

        if (L > 99.9999999) {
            return new float[]{100, 0, H};
        }

        if (L < 0.00000001) {
            return new float[]{0, 0, H};
        }

        float max = maxSafeChromaForL(L);
        float C = max / 100 * S;

        return new float[]{L, C, H};
    }

    public static float[] lchToHpluv(float[] tuple) {
        float L = tuple[0];
        float C = tuple[1];
        float H = tuple[2];

        if (L > 99.9999999) {
            return new float[]{H, 0, 100};
        }

        if (L < 0.00000001) {
            return new float[]{H, 0, 0};
        }

        float max = maxSafeChromaForL(L);
        float S = C / max * 100;

        return new float[]{H, S, L};
    }

    public static String rgbToHex(float[] tuple) {
        int[] prepared = rgbPrepare(tuple);

        return String.format("#%02x%02x%02x",
                prepared[0],
                prepared[1],
                prepared[2]);
    }

    public static float[] hexToRgb(String hex) {
        return new float[]
                {
                        Integer.parseInt(hex.substring(1, 3), 16) / 255.0f,
                        Integer.parseInt(hex.substring(3, 5), 16) / 255.0f,
                        Integer.parseInt(hex.substring(5, 7), 16) / 255.0f,
                };
    }

    public static float[] lchToRgb(float[] tuple) {
        return xyzToRgb(luvToXyz(lchToLuv(tuple)));
    }

    public static float[] rgbToLch(float[] tuple) {
        return luvToLch(xyzToLuv(rgbToXyz(tuple)));
    }

    // RGB <--> HUSL(p)

    public static float[] hsluvToRgb(float[] tuple) {
        return lchToRgb(hsluvToLch(tuple));
    }

    public static float[] rgbToHsluv(float[] tuple) {
        return lchToHsluv(rgbToLch(tuple));
    }

    public static float[] hpluvToRgb(float[] tuple) {
        return lchToRgb(hpluvToLch(tuple));
    }

    public static float[] rgbToHpluv(float[] tuple) {
        return lchToHpluv(rgbToLch(tuple));
    }

    // Hex

    public static String hsluvToHex(float[] tuple) {
        return rgbToHex(hsluvToRgb(tuple));
    }

    public static String hpluvToHex(float[] tuple) {
        return rgbToHex(hpluvToRgb(tuple));
    }

    public static float[] hexToHsluv(String s) {
        return rgbToHsluv(hexToRgb(s));
    }

    public static float[] hexToHpluv(String s) {
        return rgbToHpluv(hexToRgb(s));
    }

}
