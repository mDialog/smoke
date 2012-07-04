package smoke.mongrel2

import java.net.URI
import org.scalatest.FunSpec
import io.Source

class Mongrel2RequestTest extends FunSpec {
  // Message Fixture (new lines added for readability):
  // 54c6755b-9628-40a4-9a2d-cc82a816345e 57 /A134/B987 
  // 669:{
  //   "PATH":"/A134/B987",
  //   "accept-language":"en-US,en;q=0.8",
  //   "user-agent":"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_6_7) AppleWebKit/534.24 (KHTML, like Gecko) Chrome/11.0.696.57 Safari/534.24",
  //   "host":"test.mdialog.com:6767",
  //   "accept-charset":"ISO-8859-1,utf-8;q=0.7,*;q=0.3",
  //   "accept":"application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5",
  //   "x-forwarded-for":"127.0.0.1",
  //   "accept-encoding":"gzip,deflate,sdch",
  //   "connection":"keep-alive",
  //   "content-type":"application/x-www-form-urlencoded",
  //   "METHOD":"POST",
  //   "VERSION":"HTTP/1.1",
  //   "URI":"/A134/B987?someVal=45.432&other+val=47.334",
  //   "QUERY":"someVal=45.432&other+val=47.334",
  //   "PATTERN":"/search/"
  // },22:greeting=hello+goodbye,
  val message = Source.fromURL(getClass.getResource("/mongrel2_request.txt"))
    .getLines
    .mkString
    .getBytes

  it("should parse from Mongrel message") {
    val request = Mongrel2Request(message)
    assert(request.sender === "54c6755b-9628-40a4-9a2d-cc82a816345e")
    assert(request.connection === "57")

    assert(request.method === "POST")
    assert(request.uri === new URI("/A134/B987?someVal=45.432&other+val=47.334"))
    assert(request.path === "/A134/B987")
    assert(request.hostWithPort === "test.mdialog.com:6767")
    assert(request.host === "test.mdialog.com")
    assert(request.port === Some(6767))
    assert(request.ip === "127.0.0.1")
    assert(request.queryString === Some("someVal=45.432&other+val=47.334"))

    assert(request.contentType === Some("application/x-www-form-urlencoded"))
    assert(request.userAgent === Some("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_6_7) AppleWebKit/534.24 (KHTML, like Gecko) Chrome/11.0.696.57 Safari/534.24"))

    assert(request.body === "greeting=hello+goodbye")
    assert(request.contentLength === 22)

    assert(request.params.get("someVal") === Some("45.432"))
    assert(request.params.get("other val") === Some("47.334"))
    assert(request.params.get("greeting") === Some("hello goodbye"))
  }

}