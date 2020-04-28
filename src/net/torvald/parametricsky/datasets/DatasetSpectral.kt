/*
This source is published under the following 3-clause BSD license.

Copyright (c) 2012 - 2013, Lukas Hosek and Alexander Wilkie
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright
  notice, this list of conditions and the following disclaimer.
* Redistributions in binary form must reproduce the above copyright
  notice, this list of conditions and the following disclaimer in the
  documentation and/or other materials provided with the distribution.
* None of the names of the contributors may be used to endorse or promote
  products derived from this software without specific prior written
  permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES
LOSS OF USE, DATA, OR PROFITS OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/


/* ============================================================================

This file is part of a sample implementation of the analytical skylight and
solar radiance models presented in the SIGGRAPH 2012 paper


           "An Analytic Model for Full Spectral Sky-Dome Radiance"

and the 2013 IEEE CG&A paper

       "Adding a Solar Radiance Function to the Hosek Skylight Model"

                                   both by

                       Lukas Hosek and Alexander Wilkie
                Charles University in Prague, Czech Republic


                        Version: 1.4a, February 22nd, 2013

Version history:

1.4a  February 22nd, 2013
      Removed unnecessary and counter-intuitive solar radius parameters
      from the interface of the colourspace sky dome initialisation functions.

1.4   February 11th, 2013
      Fixed a bug which caused the relative brightness of the solar disc
      and the sky dome to be off by a factor of about 6. The sun was too
      bright: this affected both normal and alien sun scenarios. The
      coefficients of the solar radiance function were changed to fix this.

1.3   January 21st, 2013 (not released to the public)
      Added support for solar discs that are not exactly the same size as
      the terrestrial sun. Also added support for suns with a different
      emission spectrum ("Alien World" functionality).

1.2a  December 18th, 2012
      Fixed a mistake and some inaccuracies in the solar radiance function
      explanations found in ArHosekSkyModel.h. The actual source code is
      unchanged compared to version 1.2.

1.2   December 17th, 2012
      Native RGB data and a solar radiance function that matches the turbidity
      conditions were added.

1.1   September 2012
      The coefficients of the spectral model are now scaled so that the output
      is given in physical units: W / (m^-2 * sr * nm). Also, the output of the
      XYZ model is now no longer scaled to the range [0...1]. Instead, it is
      the result of a simple conversion from spectral data via the CIE 2 degree
      standard observer matching functions. Therefore, after multiplication
      with 683 lm / W, the Y channel now corresponds to luminance in lm.

1.0   May 11th, 2012
      Initial release.


Please visit http://cgg.mff.cuni.cz/projects/SkylightModelling/ to check if
an updated version of this code has been published!

============================================================================ */


/*

This file contains the coefficient data for the spectral version of the model.

*/

// uses Apr 26 dataset

package net.torvald.parametricsky.datasets

import net.torvald.parametricsky.datasets.DatasetOp.readDatasetFromFile
import kotlin.test.assertEquals

object DatasetSpectral {

    val dataset320 = readDatasetFromFile("./work_files/skylight/hosek_model_source/dataset320.bin")
    val dataset360 = readDatasetFromFile("./work_files/skylight/hosek_model_source/dataset360.bin")
    val dataset400 = readDatasetFromFile("./work_files/skylight/hosek_model_source/dataset400.bin")
    val dataset440 = readDatasetFromFile("./work_files/skylight/hosek_model_source/dataset440.bin")
    val dataset480 = readDatasetFromFile("./work_files/skylight/hosek_model_source/dataset480.bin")
    val dataset520 = readDatasetFromFile("./work_files/skylight/hosek_model_source/dataset520.bin")
    val dataset560 = readDatasetFromFile("./work_files/skylight/hosek_model_source/dataset560.bin")
    val dataset600 = readDatasetFromFile("./work_files/skylight/hosek_model_source/dataset600.bin")
    val dataset640 = readDatasetFromFile("./work_files/skylight/hosek_model_source/dataset640.bin")
    val dataset680 = readDatasetFromFile("./work_files/skylight/hosek_model_source/dataset680.bin")
    val dataset720 = readDatasetFromFile("./work_files/skylight/hosek_model_source/dataset720.bin")

    val datasetRad320 = readDatasetFromFile("./work_files/skylight/hosek_model_source/datasetRad320.bin")
    val datasetRad360 = readDatasetFromFile("./work_files/skylight/hosek_model_source/datasetRad360.bin")
    val datasetRad400 = readDatasetFromFile("./work_files/skylight/hosek_model_source/datasetRad400.bin")
    val datasetRad440 = readDatasetFromFile("./work_files/skylight/hosek_model_source/datasetRad440.bin")
    val datasetRad480 = readDatasetFromFile("./work_files/skylight/hosek_model_source/datasetRad480.bin")
    val datasetRad520 = readDatasetFromFile("./work_files/skylight/hosek_model_source/datasetRad520.bin")
    val datasetRad560 = readDatasetFromFile("./work_files/skylight/hosek_model_source/datasetRad560.bin")
    val datasetRad600 = readDatasetFromFile("./work_files/skylight/hosek_model_source/datasetRad600.bin")
    val datasetRad640 = readDatasetFromFile("./work_files/skylight/hosek_model_source/datasetRad640.bin")
    val datasetRad680 = readDatasetFromFile("./work_files/skylight/hosek_model_source/datasetRad680.bin")
    val datasetRad720 = readDatasetFromFile("./work_files/skylight/hosek_model_source/datasetRad720.bin")

    val solarDataset320 = readDatasetFromFile("./work_files/skylight/hosek_model_source/solarDataset320.bin")
    val solarDataset360 = readDatasetFromFile("./work_files/skylight/hosek_model_source/solarDataset360.bin")
    val solarDataset400 = readDatasetFromFile("./work_files/skylight/hosek_model_source/solarDataset400.bin")
    val solarDataset440 = readDatasetFromFile("./work_files/skylight/hosek_model_source/solarDataset440.bin")
    val solarDataset480 = readDatasetFromFile("./work_files/skylight/hosek_model_source/solarDataset480.bin")
    val solarDataset520 = readDatasetFromFile("./work_files/skylight/hosek_model_source/solarDataset520.bin")
    val solarDataset560 = readDatasetFromFile("./work_files/skylight/hosek_model_source/solarDataset560.bin")
    val solarDataset600 = readDatasetFromFile("./work_files/skylight/hosek_model_source/solarDataset600.bin")
    val solarDataset640 = readDatasetFromFile("./work_files/skylight/hosek_model_source/solarDataset640.bin")
    val solarDataset680 = readDatasetFromFile("./work_files/skylight/hosek_model_source/solarDataset680.bin")
    val solarDataset720 = readDatasetFromFile("./work_files/skylight/hosek_model_source/solarDataset720.bin")

    init {
        assertEquals(1080, dataset600.size, "Dataset size mismatch - expected 1080, got ${dataset600.size}")
        assertEquals(120, datasetRad600.size, "Dataset size mismatch - expected 120, got ${datasetRad600.size}")
        assertEquals(1800, solarDataset600.size, "Dataset size mismatch - expected 1800, got ${solarDataset600.size}")

        assertEquals(-1.341049e+001, dataset320[0], "Dataset not parsed correctly - expected ${-1.341049e+001}, got ${dataset320[0]}")
    }

    val datasets = arrayOf(
            dataset320,
            dataset360,
            dataset400,
            dataset440,
            dataset480,
            dataset520,
            dataset560,
            dataset600,
            dataset640,
            dataset680,
            dataset720
    )

    val datasetsRad = arrayOf(
            datasetRad320,
            datasetRad360,
            datasetRad400,
            datasetRad440,
            datasetRad480,
            datasetRad520,
            datasetRad560,
            datasetRad600,
            datasetRad640,
            datasetRad680,
            datasetRad720
    )

    val solarDatasets = arrayOf(
            solarDataset320,
            solarDataset360,
            solarDataset400,
            solarDataset440,
            solarDataset480,
            solarDataset520,
            solarDataset560,
            solarDataset600,
            solarDataset640,
            solarDataset680,
            solarDataset720
    )

    val waves = floatArrayOf(320f, 360f, 400f, 440f, 480f, 520f, 560f, 600f, 640f, 680f, 720f)

    val limbDarkeningDataset320 = doubleArrayOf(0.087657, 0.767174, 0.658123, -1.02953, 0.703297, -0.186735)

    val limbDarkeningDataset360 = doubleArrayOf(0.122953, 1.01278, 0.238687, -1.12208, 1.17087, -0.424947)

    val limbDarkeningDataset400 = doubleArrayOf(0.123511, 1.08444, -0.405598, 0.370629, -0.240567, 0.0674778)

    val limbDarkeningDataset440 = doubleArrayOf(0.158489, 1.23346, -0.875754, 0.857812, -0.484919, 0.110895)

    val limbDarkeningDataset480 = doubleArrayOf(0.198587, 1.30507, -1.25998, 1.49727, -1.04047, 0.299516)

    val limbDarkeningDataset520 = doubleArrayOf(0.23695, 1.29927, -1.28034, 1.37760, -0.85054, 0.21706)

    val limbDarkeningDataset560 = doubleArrayOf(0.26892, 1.34319, -1.58427, 1.91271, -1.31350, 0.37295)

    val limbDarkeningDataset600 = doubleArrayOf(0.299804, 1.36718, -1.80884, 2.29294, -1.60595, 0.454874)

    val limbDarkeningDataset640 = doubleArrayOf(0.33551, 1.30791, -1.79382, 2.44646, -1.89082, 0.594769)

    val limbDarkeningDataset680 = doubleArrayOf(0.364007, 1.27316, -1.73824, 2.28535, -1.70203, 0.517758)

    val limbDarkeningDataset720 = doubleArrayOf(0.389704, 1.2448, -1.69708, 2.14061, -1.51803, 0.440004)

    val limbDarkeningDatasets = arrayOf(
            limbDarkeningDataset320,
            limbDarkeningDataset360,
            limbDarkeningDataset400,
            limbDarkeningDataset440,
            limbDarkeningDataset480,
            limbDarkeningDataset520,
            limbDarkeningDataset560,
            limbDarkeningDataset600,
            limbDarkeningDataset640,
            limbDarkeningDataset680,
            limbDarkeningDataset720
    )
}