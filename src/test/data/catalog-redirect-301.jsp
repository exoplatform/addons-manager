<%
  response.setStatus(301);
  response.setHeader("Location", "/catalog.json");
  response.setHeader("Connection", "close");
%>