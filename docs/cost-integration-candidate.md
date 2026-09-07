# Cost integration candidate

This branch contains the explicitly listed restaurant cost-chain candidate in
cost-integration-candidate.json. It is not a full system release baseline.

Included: receipt, requisition, processing and costing implementation, required
entity mappings and repositories, selected frontend changes, synthetic database
fixture and dedicated tests. No legal, authentication or production configuration
files were imported from the shared working tree.

Validation command from banquet_project:
YOUJIAN_TEST_MYSQL=1 mvn -B -o -Dtest=SupplyCostMysqlIntegrationTest,CostFailureTest test

The browser acceptance case requires a separate environment flag and its browser
assets. A skipped browser test is not passed acceptance. Production source/schema
comparison, remaining booking repairs and other agents' patches are still pending.
