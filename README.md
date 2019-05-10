## Deploying riff function pets with service binding

### prerequisites

1. A [Knative Serving installation](https://github.com/knative/docs/blob/master/install/README.md) version 0.5 or later

2. The `riff` binary, install [latest release](https://github.com/projectriff/riff/releases)

3. Kubernetes Service Catalog installed, install [using Helm](https://kubernetes.io/docs/tasks/service-catalog/install-service-catalog-using-helm/)
  run:
  ```
  helm install . --name catalog --namespace catalog
  ```

4. The GCP Service Broker installed from the [develop branch](https://github.com/GoogleCloudPlatform/gcp-service-broker/tree/develop/deployments/helm/gcp-service-broker)
  run:
  ```
  helm dependency update
  helm install . --name broker --namespace catalog
  ```

5. The `svcat` CLI [installed](https://github.com/kubernetes-incubator/service-catalog/blob/master/docs/install.md#manual)

### sample Spring Boot function app deployment

#### enable Istio Egress for GCP

First the Google Cloud Platform APIs:
```
cat <<EOF | kubectl apply -f -
apiVersion: networking.istio.io/v1alpha3
kind: ServiceEntry
metadata:
  name: googleapis
spec:
  hosts:
  - "*.googleapis.com"
  location: MESH_EXTERNAL
  ports:
  - number: 443
    name: https
    protocol: HTTPS
EOF
```

And then the metadata server:
```
cat <<EOF | kubectl apply -f -
apiVersion: networking.istio.io/v1alpha3
kind: ServiceEntry
metadata:
  name: metadata-server
spec:
  hosts:
  - metadata.google.internal
  - 169.254.169.254
  location: MESH_EXTERNAL
  ports:
  - number: 80
    name: http
    protocol: HTTP
EOF
```

#### provision a MySQL database and create the svcat binding

```
svcat provision petdb --class google-cloudsql-mysql --plan mysql-db-n1-standard-1
svcat bind petdb --name riff-binding-pets-db --secret-name riff-binding-pets-petdb
```

#### enable Istio Egress to Google Cloud SQL instance:
```
cat <<EOF | kubectl apply -f -
apiVersion: networking.istio.io/v1alpha3
kind: ServiceEntry
metadata:
  name: cloud-sql
spec:
  hosts:
  - $(kubectl get secret riff-binding-pets-petdb -ojson | jq -r '.data.host' | base64 -D)
  location: MESH_EXTERNAL
  ports:
  - number: 3307
    name: mysql
    protocol: TCP
EOF
```

#### checkout and prepare the function

Checkout the [riff pets function](https://github.com/trisberg/pets)

#### deploy the function:

change to the `pets` directory:

```
cd pets
```

deploy the function

```
riff function create pets --local-path . \
--env SPRING_CLOUD_GCP_PROJECT_ID='${GCP_PROJECT_ID}' \
--env-from GCP_PROJECT_ID=secretKeyRef:riff-binding-pets-petdb:ProjectId \
--env-from GCP_REGION=secretKeyRef:riff-binding-pets-petdb:region \
--env-from GCP_INSTANCE=secretKeyRef:riff-binding-pets-petdb:instance_name \
--env SPRING_CLOUD_GCP_SQL_INSTANCE_CONNECTION_NAME='${GCP_PROJECT_ID}:${GCP_REGION}:${GCP_INSTANCE}'
```

#### invoke the function:

```
riff service invoke pets --text -- -w '\n' -d cat
```
