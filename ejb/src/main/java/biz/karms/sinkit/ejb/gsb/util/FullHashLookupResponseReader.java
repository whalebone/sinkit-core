package biz.karms.sinkit.ejb.gsb.util;

import biz.karms.sinkit.ejb.gsb.dto.FullHashLookupResponse;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.ArrayList;

/**
 * Created by tom on 12/13/15.
 *
 * @author Tomas Kozel
 */
public class FullHashLookupResponseReader implements MessageBodyReader<FullHashLookupResponse> {

    public static final int LINE_FEED = 10;

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == FullHashLookupResponse.class && MediaType.APPLICATION_OCTET_STREAM_TYPE.equals(mediaType);
    }

    @Override
    public FullHashLookupResponse readFrom(Class<FullHashLookupResponse> type, Type genericType, Annotation[] annotations,
                                           MediaType mediaType, MultivaluedMap<String, String> httpHeaders,
                                           InputStream entityStream) throws IOException, WebApplicationException {
        BufferedInputStream bis = new BufferedInputStream(entityStream);
        FullHashLookupResponse response = new FullHashLookupResponse();
        String cacheLifetime = readLine(bis);

        try {
            int initSeconds = Integer.parseInt(cacheLifetime);
            response.setValidSeconds(initSeconds);
        } catch (NumberFormatException e) {
            throw new WebApplicationException("Cannot parse cache validity field: " + cacheLifetime);
        }

        String line;
        String[] hashEntryHeaders;
        String listName;
        int hashLength;
        int hashCount;
        byte[] hash;
        AbstractMap.SimpleEntry<byte[], byte[]> hashMetadataPair;

        boolean isMetadata;
        int metadataLength;
        byte[] metadata;

        while ((line = readLine(bis)) != null) {
            isMetadata = false;
            hashEntryHeaders = line.split(":");
            if (hashEntryHeaders.length != 3 && hashEntryHeaders.length != 4) {
                throw new WebApplicationException("Cannot parse cache entry headers: " + line);
            }
            if (hashEntryHeaders.length == 4) {
                if (!"m".equals(hashEntryHeaders[3])) {
                    throw new WebApplicationException("Don't understand metadata header: " + hashEntryHeaders[3]);
                }
                isMetadata = true;
            }
            listName = hashEntryHeaders[0];
            hashLength = Integer.parseInt(hashEntryHeaders[1]);
            hashCount = Integer.parseInt(hashEntryHeaders[2]);
            for (int i = 0; i < hashCount; i++) {
                hash = new byte[hashLength];
                if (bis.read(hash) == -1) {
                    throw new WebApplicationException("Unexpected EOF, hashes cannot be parsed");
                }
                hashMetadataPair = new AbstractMap.SimpleEntry<>(hash, null);
                ArrayList<AbstractMap.SimpleEntry<byte[], byte[]>> blacklist = response.getFullHashes().get(listName);
                if (blacklist == null) {
                    blacklist = new ArrayList<>();
                    response.getFullHashes().put(listName, blacklist);
                }
                blacklist.add(hashMetadataPair);
            }

            if (isMetadata) {
                for (int i = 0; i < hashCount; i++) {
                    metadataLength = Integer.parseInt(readLine(bis));
                    metadata = new byte[metadataLength];
                    if (bis.read(metadata) == -1) {
                        throw new WebApplicationException("Problem parsing metadata.");
                    }
                    response.getFullHashes().get(listName).get(i).setValue(metadata);
                }
            }
        }
        return response;

    }

    private String readLine(BufferedInputStream bis) throws WebApplicationException, IOException {
        String line = null;
        int c;
        while ((c = bis.read()) != -1) {
            if (c == LINE_FEED) break;
            if (line == null) {
                line = "" + (char) c;
            } else {
                line += (char) c;
            }
        }
        return line;
    }
}
