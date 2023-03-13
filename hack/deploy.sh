echo
echo "Start deploying kubernetes deployments"
echo

kubectl apply -f deployment/k8s/k8s-deployment.yml

echo
echo "Start deploying kubernetes services"
echo

kubectl apply -f deployment/k8s/k8s-service.yml