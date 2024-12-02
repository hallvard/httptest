package no.hal.httpfile;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Function;

public enum Functions implements Function<List<String>, String> {

    
    // {{$guid}}
    guid {
        @Override
        public String apply(List<String> args) {
            return UUID.randomUUID().toString();
        }
    },
    // {{$randomInt min max}}
    randomInt {
        @Override
        public String apply(List<String> args) {
            int min = Integer.parseInt(args.get(0));
            int max = Integer.parseInt(args.get(1));
            return Integer.toString(min + (int) (Math.random() * (max - min)));
        }
    },
    // {{$timestamp [offset option]}}
    timestamp {
        @Override
        public String apply(List<String> args) {
            int offset = args.size() >= 1 ? Integer.parseInt(args.get(0)) : 0;
            var datetime = LocalDateTime.now().plusSeconds(offset);
            var timestamp = datetime.toEpochSecond(ZoneOffset.ofTotalSeconds(0));
            return String.valueOf(timestamp);
        }
    },
    // {{$datetime rfc1123|iso8601 [offset option]}}
    datetime {
        @Override
        public String apply(List<String> args) {
            int offset = args.size() >= 2 ? Integer.parseInt(args.get(1)) : 0;
            var datetime = LocalDateTime.now().plusSeconds(offset);
            return String.valueOf(datetime);
        }
    },
    // {{$localDatetime rfc1123|iso8601 [offset option]}}
    localDatetime {
        @Override
        public String apply(List<String> args) {
            int offset = args.size() >= 2 ? Integer.parseInt(args.get(1)) : 0;
            var datetime = LocalDateTime.now().plusSeconds(offset);
            return String.valueOf(datetime);
        }
    },
    // {{$processEnv [%]envVarName}}
    processEnv {
        @Override
        public String apply(List<String> args) {
            var envVarName = args.get(0);
            if (envVarName.startsWith("%")) {
                envVarName = envVarName.substring(1);
            }
            var envVarValue = System.getenv(envVarName);
            return envVarValue != null ? envVarValue : "";
        }
    },
    // {{$dotenv [%]variableName}}
    dotenv {
        @Override
        public String apply(List<String> args) {
            var envProps = new Properties();
            try {
                envProps.load(new FileInputStream(".env"));
            } catch (IOException e) {
                // ignore
            }
            var envVarName = args.get(0);
            if (envVarName.startsWith("%")) {
                envVarName = envVarName.substring(1);
            }
            var envVarValue = envProps.getProperty(envVarName);
            return envVarValue != null ? envVarValue : "";
        }
    },
    // {{$aadToken [new] [public|cn|de|us|ppe] [<domain|tenantId>] [aud:<domain|tenantId>]}}
}   
