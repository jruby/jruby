REPORT = {old_api: [<#list analysis.oldApi.archives as archive>%q{${archive.name}}<#sep>, </#list>],
 new_api: [<#list analysis.newApi.archives as archive>%q{${archive.name}}<#sep>, </#list>],
 report: [<#list reports as report> 
  {
   old: %q{${report.oldElement!"nil"}},
   new: %q{${report.newElement!"nil"}},
   diff: [<#list report.differences as diff>
            [
             %q{${diff.code}},
             %q{${diff.description!"nil"}},
             {
               <#list diff.classification?keys as compat>
               %q{${compat}} => "${diff.classification?api.get(compat)}",
               </#list>
             },
            ],
           </#list>
         ],
  },
</#list>
 ]
}
