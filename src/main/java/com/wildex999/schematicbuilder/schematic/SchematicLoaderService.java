package com.wildex999.schematicbuilder.schematic;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.mutable.MutableInt;

import com.wildex999.schematicbuilder.ModSchematicBuilder;
import com.wildex999.schematicbuilder.tiles.BuilderState;
import com.wildex999.utils.ModLog;

/*
 * Service for loading Schematics in Threads.
 * Can also in the future allow for caching of Schematics that are used often/multiple places.
 */

public class SchematicLoaderService {

	public static SchematicLoaderService instance;
	private final ExecutorService pool;
	
	public SchematicLoaderService (int maxThreads) {
		this.instance = this;
		pool = Executors.newFixedThreadPool(maxThreads);
	}
	
	public static class Result {
		public Schematic schematic;
		public String message; //Message is warning if schematic is non-null, or an error if schematic is null.
		public HashMap<Short, MutableInt> blockCount;
	}	
	
	//Load a compressed serialized Schematic from data
	public Future<Result> loadSerialized(byte[] data, HashMap<Short, MutableInt> blockCount) {
		return pool.submit(new SerializedReadWorker(data, blockCount));
	}
	
	private class SerializedReadWorker implements Callable<Result> {

		private byte[] data;
		private Schematic schematic;
		HashMap<Short, MutableInt> blockCount;
		
		public SerializedReadWorker(byte[] data, HashMap<Short, MutableInt> blockCount) {
			this.data = data;
			this.blockCount = blockCount;
		}
		
		@Override
		public Result call() throws Exception {
			long timeStart = System.nanoTime();
			ByteArrayOutputStream decompressedStream = new ByteArrayOutputStream();
			
			IOUtils.copy(new GZIPInputStream(new ByteArrayInputStream(data)), decompressedStream);
			ByteBuf buf = Unpooled.wrappedBuffer(decompressedStream.toByteArray());

			if(ModSchematicBuilder.debug)
			{
				long timeEnd = System.nanoTime();
				System.out.println("Time spent decompressing uploaded Schematic: " + TimeUnit.MILLISECONDS.convert(timeEnd-timeStart, TimeUnit.NANOSECONDS) + " ms");
			}
			
			
			String errorMessage = null;
			try {
				schematic = Schematic.fromBytes(buf, blockCount);
			} catch(Exception e) {
				schematic = null;
				errorMessage = "Error parsing serialized Schematic: " + e.getMessage();
				System.err.println(errorMessage);
			}
			
			//Prepare answer
			Result result = new Result();
			result.schematic = schematic;
			if(schematic == null)
			{
				result.message = errorMessage;
			}
			else
			{
				result.message = null;
				result.blockCount = blockCount;
			}
			
			return result;
		}
		
	}
	
	//Load a Schematic from file
	public Future<Result> loadFile(File file, HashMap<Short, MutableInt> blockCount) {
		return pool.submit(new FileReadWorker(file, blockCount));
	}
	
	private class FileReadWorker implements Callable<Result> {

		private File file;
		private HashMap<Short, MutableInt> blockCount;
		
		public FileReadWorker(File file, HashMap<Short, MutableInt> blockCount) {
			this.file = file;
			this.blockCount = blockCount;
		}
		
		@Override
		public Result call() throws Exception {
			Result result = new Result();
			result.schematic = null;
			result.message = "";
			result.blockCount = blockCount;
			
			try {
				result.schematic = SchematicLoader.loadSchematic(file, blockCount);
			} catch (Exception e) {
				result.message = "Failed to load cached Schematic: " + file;
				e.printStackTrace();
			}
			
			return result;
		}
		
	}
	
	public Future<Result> writeFile() {
		return null;
	}
}
