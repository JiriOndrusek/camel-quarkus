[camel-quarkus-extensions]
= Camel Quarkus extensions reference
:page-aliases: list-of-camel-quarkus-extensions.adoc,reference/extensions/index.adoc

[TIP]
====
In case you are missing some extension in the list:

* Upvote https://github.com/apache/camel-quarkus/issues[an existing issue] or create
  https://github.com/apache/camel-quarkus/issues/new[a new one] so that we can better prioritize our work.
* You may also want to try to add the extension yourself following our xref:contributor-guide/index.adoc[Contributor guide].
* You may try your luck using the given camel component on Quarkus directly (without an extension). Most probably it
  will work in the JVM mode and fail in the native mode. Do not hesitate to
  https://github.com/apache/camel-quarkus/issues[report] any issues you encounter.
====

[=components?size] extensions ([=numberOfDeprecated] deprecated, [=numberofJvmOnly] JVM only)

[width="100%",cols="4,1,1,1,5",options="header"]
|===
| Extension | Artifact | Support Level | Since | Description
[#list components as row]

| [#if getDocLink(row)??] [=getDocLink(row)][[=row.title]] [#else] ([=row.title])[/#if] | [=row.artifactId] | [=getTarget(row)] +
[=getSupportLevel(row)] | [=row.firstVersion] | [#if row.deprecated]*deprecated* [/#if][=row.description]
[/#list]
|===
