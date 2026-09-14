package com.dji.sample.service;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.lang.reflect.*;
import static org.junit.jupiter.api.Assertions.*;
class LocalStreamArchiveTest {
 @TempDir Path dir;
 @Test void readsArchiveAndRejectsEscapePaths() throws Exception {
  Path root=Files.createDirectory(dir.resolve("archive"));
  Files.writeString(root.resolve("info.json"),"local-data");
  Path outside=Files.writeString(dir.resolve("outside.json"),"outside");
  Files.createSymbolicLink(root.resolve("escape.json"),outside);
  S3PresignService service=new S3PresignService("ap-northeast-2");
  Field field=S3PresignService.class.getDeclaredField("localStreamDir");field.setAccessible(true);field.set(service,root.toString());
  try(var in=service.getStreamObject("info.json")){assertEquals("local-data",new String(in.readAllBytes()));}
  assertTrue(service.streamObjectExists("info.json"));
  Method find=S3PresignService.class.getDeclaredMethod("localStreamFile",String.class);find.setAccessible(true);
  assertNull(find.invoke(service,"../outside.json"));
  assertNull(find.invoke(service,"escape.json"));
  assertNull(find.invoke(service,"absent.json"));
 }
}
