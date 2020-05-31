/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.sysds.test.component.compress;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.sysds.runtime.compress.colgroup.ColGroup.CompressionType;
import org.apache.sysds.runtime.matrix.data.MatrixBlock;
import org.apache.sysds.runtime.util.DataConverter;

/**
 * WARNING, this compressible input generator generates transposed inputs, (rows
 * and cols are switched) this is because then the test does not need to
 * transpose the input for the colGroups that expect transposed inputs.
 * 
 */
public class CompressibleInputGenerator {

	public static MatrixBlock getInput(int rows, int cols, CompressionType ct, int nrUnique, 
			double sparsity, int seed) {
		double[][] output = getInputDoubleMatrix(rows, cols, ct, nrUnique, 1000000, -1000000, sparsity, seed, false);
		return DataConverter.convertToMatrixBlock(output);
	}

	public static MatrixBlock getInput(int rows, int cols, CompressionType ct, int nrUnique, int max, int min,
			double sparsity, int seed) {
		double[][] output = getInputDoubleMatrix(rows, cols, ct, nrUnique, max, min, sparsity, seed, false);
		return DataConverter.convertToMatrixBlock(output);
	}

	public static double[][] getInputDoubleMatrix(int rows, int cols, CompressionType ct, int nrUnique, int max,
			int min, double sparsity, int seed, boolean transpose) {
		double[][] output;
		switch (ct) {
			case RLE:
				output = rle(rows, cols, nrUnique, max, min, sparsity, seed, transpose);
				break;
			case OLE:
				output = ole(rows, cols, nrUnique, max, min, sparsity, seed, transpose);
				break;
			default:
				throw new NotImplementedException("Not implemented generator.");
		}
		for(double[] x : output){
			DoubleSummaryStatistics dss =  Arrays.stream(x).summaryStatistics();
			if(dss.getMax() > max) {
				throw new RuntimeException("Incorrect matrix generated "+ct+", max to high was: " + dss.getMax() + " should be :" + max);
			}
			if(dss.getMin() < min) {
				throw new RuntimeException("Incorrect matrix generated "+ct+", min to low was: " + dss.getMin()  + " should be :" + min);
			}
		}
		return output;
	}

	private static double[][] rle(int rows, int cols, int nrUnique, int max, int min, double sparsity, int seed,
			boolean transpose) {

		Random r = new Random(seed);
		List<Double> values = getNRandomValues(nrUnique, r, max, min);

		double[][] matrix = transpose ? new double[rows][cols] : new double[cols][rows];

		for (int colNr = 0; colNr < cols; colNr++) {
			Collections.shuffle(values, r);

			// Generate a Dirichlet distribution, to distribute the values
			int[] occurences = makeDirichletDistribution(nrUnique, rows, r);

			// double[] col = new double[rows];

			int pointer = 0;
			int valuePointer = 0;
			for (int nr : occurences) {
				int zeros = (int) (Math.floor(nr * (1.0 - sparsity)));
				int before = (zeros > 0) ? r.nextInt(zeros) : 0;
				int after = zeros - before;
				pointer += before;
				for (int i = before; i < nr - after; i++) {
					if (transpose) {
						matrix[pointer][colNr] = values.get(valuePointer);
					} else {
						matrix[colNr][pointer] = values.get(valuePointer);
					}
					pointer++;
				}
				pointer += after;
				valuePointer++;
				if (valuePointer == values.size() && after == 0) {
					while (pointer < rows) {
						if (transpose) {
							matrix[pointer][colNr] = values.get(nrUnique - 1);
						} else {
							matrix[colNr][pointer] = values.get(nrUnique - 1);
						}
						pointer++;
					}
				}
			}
		}
		return matrix;
	}

	/**
	 * Note ole compress the best if there are multiple correlated columns.
	 * Therefore the multiple columns are needed for good compressions. Also Nr
	 * Unique is only associated to a specific column in this compression, so the
	 * number of uniques are only in a single column, making actual the nrUnique
	 * (cols * nrUnique) Does not guaranty that all the nr uniques are in use, since
	 * the values are randomly selected.
	 * 
	 * @param rows      Number of rows in generated output
	 * @param cols      Number of cols in generated output
	 * @param nrUnique  Number of unique values in generated output, Note this means
	 *                  base unique in this case. and this number will grow
	 *                  according to sparsity as well.
	 * @param max       The Maximum Value contained
	 * @param min       The Minimum value contained
	 * @param sparsity  The sparsity of the generated matrix
	 * @param seed      The seed of the generated matrix
	 * @param transpose If the output should be a transposed matrix or not
	 * @return Generated nicely compressible OLE col Group.
	 */
	private static double[][] ole(int rows, int cols, int nrUnique, int max, int min, double sparsity, int seed,
			boolean transpose) {
		// chose some random values
		Random r = new Random(seed);
		List<Double> values = getNRandomValues(nrUnique, r, max, min);
		double[][] matrix = transpose ? new double[rows][cols] : new double[cols][rows];

		// Generate the first column.
		for (int x = 0; x < rows; x++) {
			if (r.nextDouble() < sparsity) {
				if (transpose) {
					matrix[x][0] = values.get(r.nextInt(nrUnique));
				} else {
					matrix[0][x] = values.get(r.nextInt(nrUnique));
				}
			}
		}

		for (int y = 1; y < cols; y++) {
			for (int x = 0; x < rows; x++) {
				if (r.nextDouble() < sparsity) {
					if (transpose) {
						matrix[x][y] = matrix[x][0];
					} else {
						matrix[y][x] = matrix[0][x];
					}
				}
			}
		}
		return matrix;
	}

	private static int[] makeDirichletDistribution(int nrUnique, int rows, Random r) {
		double[] distribution = new double[nrUnique];
		double sum = 0;
		for (int i = 0; i < nrUnique; i++) {
			distribution[i] = r.nextDouble();
			sum += distribution[i];
		}

		int[] occurences = new int[nrUnique];
		for (int i = 0; i < nrUnique; i++) {
			occurences[i] = (int) (((double) distribution[i] / (double) sum) * (double) rows);
		}
		return occurences;
	}

	private static List<Double> getNRandomValues(int nrUnique, Random r, int max, int min) {
		List<Double> values = new ArrayList<>();
		for (int i = 0; i < nrUnique; i++) {
			double v = (r.nextDouble() * (double)(max - min)) + (double)min;
			values.add( Math.floor(v));
		}
		return values;
	}
}
