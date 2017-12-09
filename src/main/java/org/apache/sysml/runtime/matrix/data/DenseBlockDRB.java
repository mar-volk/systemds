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


package org.apache.sysml.runtime.matrix.data;

import java.util.Arrays;

public class DenseBlockDRB extends DenseBlock
{
	private static final long serialVersionUID = 8546723684649816489L;

	private double[] data;
	private int rlen;
	private int clen;

	public DenseBlockDRB(int rlen, int clen) {
		reset(rlen, clen);
	}
	
	@Override
	public void reset() {
		reset(rlen, clen);
	}

	@Override
	public void reset(int rlen, int clen) {
		int len = rlen * clen;
		if( len < capacity() )
			Arrays.fill(data, 0, len, 0);
		else
			data = new double[len];
		this.rlen = rlen;
		this.clen = clen;
	}

	@Override
	public int numRows() {
		return rlen;
	}

	@Override
	public int numBlocks() {
		return 1;
	}

	@Override
	public long size() {
		return rlen * clen;
	}

	@Override
	public long capacity() {
		return (data!=null) ? data.length : -1;
	}

	@Override
	public long countNonZeros() {
		final int len = rlen * clen;
		int nnz = 0;
		for(int i=0; i<len; i++)
			nnz += (data[i]!=0) ? 1 : 0;
		return nnz;
	}

	@Override
	public double[][] values() {
		return new double[][]{data};
	}

	@Override
	public double[] values(int bix) {
		return data;
	}

	@Override
	public int index(int r) {
		return 0;
	}

	@Override
	public int pos(int r) {
		return r * clen;
	}

	@Override
	public int pos(int r, int c) {
		return r * clen + c;
	}

	@Override
	public void set(int r, int c, double v) {
		data[pos(r, c)] = v;
	}

	@Override
	public double get(int r, int c) {
		return data[pos(r, c)];
	}
}