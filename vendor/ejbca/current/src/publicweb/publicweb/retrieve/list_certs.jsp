<%@ include file="header.jsp" %>
  <h1 class="title">List Certificates</h1>
  <p>Enter the subject DN (e.g., &quot<code>c=SE, O=AnaTom, CN=foo</code>&quot;) to list a user's certificates.</p>
  <form action="list_certs_result.jsp" enctype="x-www-form-encoded" method="GET">
    <fieldset>
      <legend>Distinguished name</legend>
      <input type="hidden" name="cmd" value="listcerts" />
      <label for="subject">Subject DN</label>
      <input name="subject" id="subject" type="text" size="40" accesskey="s" />
      <br />
      <label for="ok"></label>
      <input type="submit" id="ok" value="OK" name="submit" />
    </fieldset>
  </form>
</div>
<%@ include file="footer.inc" %>
