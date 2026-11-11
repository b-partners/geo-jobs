sudo apt-get install jq
export API_URL_SSM="`aws ssm get-parameter --name /geo-jobs/$1/api/url`"
export API_URL=`echo $API_URL_SSM | jq -r '.Parameter.Value'`
curl --fail -H "x-api-key: $HEALTH_ADMIN_API_KEY" "$API_URL$2"
