package de.mhus.micro.karaf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.Map.Entry;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import de.mhus.lib.core.IProperties;
import de.mhus.lib.core.M;
import de.mhus.lib.core.MValidator;
import de.mhus.lib.core.MXml;
import de.mhus.lib.core.definition.DefRoot;
import de.mhus.lib.core.operation.OperationDescription;
import de.mhus.lib.core.util.ParameterDefinition;
import de.mhus.lib.core.util.ParameterDefinitions;
import de.mhus.lib.form.ModelUtil;
import de.mhus.micro.api.MicroApi;
import de.mhus.micro.api.client.MicroFilter;
import de.mhus.micro.api.client.MicroOperation;
import de.mhus.micro.api.client.PathFilter;
import de.mhus.micro.api.client.UuidFilter;
import de.mhus.osgi.api.karaf.AbstractCmd;

@Command(scope = "micro", name = "operation-inspect", description = "Operation information")
@Service
public class CmdOperationInspect extends AbstractCmd {

    @Argument(
            index = 0,
            name = "filter path",
            required = false,
            description = "",
            multiValued = false)
    String path;

    @Argument(
            index = 1,
            name = "version",
            required = false,
            description = "",
            multiValued = false)
    String version;
    
    @Option(
            name = "-l",
            aliases = {"--label"},
            description = "Filter for labels",
            required = false,
            multiValued = true)
    private String[] labels;

    @Override
    public Object execute2() throws Exception {
        MicroApi api = M.l(MicroApi.class);
        ArrayList<MicroOperation> results = new ArrayList<>();

        IProperties eLabels = IProperties.explodeToMProperties(labels);

        MicroFilter filter = null;
        if (MValidator.isUUID(path))
            filter = new UuidFilter(UUID.fromString(path), eLabels);
        else if (version != null){
            if ("*".equals(version)) version = null;
            filter = new PathFilter(path, version, eLabels);
        } else {
            filter = new MicroFilter() {
                @Override
                public boolean matches(OperationDescription desc) {
                    return path == null || desc.getPath().matches(path);
                }
            };
        }
        
        api.operations(filter, results);
        for (MicroOperation oper : results) {
            OperationDescription desc = oper.getDescription();
            System.out.println("==========================================================================");
            System.out.println("Path   : "+desc.getPath());
            System.out.println("Version: "+desc.getVersion());
            System.out.println("UUID   : "+desc.getUuid());
            System.out.println("--------------------------------------------------------------------------");
            System.out.println("Title   : " + desc.getTitle());
            System.out.println("Executor: " + oper.getClass().getCanonicalName());
            DefRoot form = desc.getForm();
            if (form != null) {
                form.build();
                System.out.println(">>> Form -----------------------------------------------------------------");
                System.out.println(MXml.toString(ModelUtil.toXml(form), true));
                System.out.println("<<< Form -----------------------------------------------------------------");
            }
            ParameterDefinitions defs = desc.getParameterDefinitions();
            if (defs != null && !defs.isEmpty()) {
                System.out.println(">>> Parameter ------------------------------------------------------------");
                for (Entry<String, ParameterDefinition> entry : defs.entrySet()) {
                    System.out.println("=== Name      : " + entry.getKey());
                    ParameterDefinition def = entry.getValue();
                    System.out.println("    Type      : " + def.getType());
                    System.out.println("    Format    : " + def.getFormat());
                    System.out.println("    Default   : " + def.getDef());
                    System.out.println("    Mapping   : " + def.getMapping());
                    System.out.println("    Properties: " + def.getProperties());
                }
                System.out.println("<<< Parameter ------------------------------------------------------------");
            }
            HashMap<String, String> labels = desc.getLabels();
            if (!labels.isEmpty()) {
                System.out.println(">>> Labels ---------------------------------------------------------------");
                for (Entry<String, String> entry : labels.entrySet())
                    System.out.println(entry.getKey() + "=" + entry.getValue());
                System.out.println(">>> Labels ---------------------------------------------------------------");
            }
            System.out.println();
        }
        return null;
    }

}
