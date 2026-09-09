import copy
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

from check import check_metadata
from test_ipad_v2_preflight import base_md, with_index, with_scope_fk


class TestCoordinatorGuard(unittest.TestCase):
    def ready(self):
        return copy.deepcopy(with_scope_fk(with_index(base_md())))

    def blocked(self, md):
        result = check_metadata(md)
        self.assertEqual('BLOCKED', result['status'])
        self.assertEqual([], result['ddl'])

    def test_same_schema_complete_is_applied(self):
        self.assertEqual('ALREADY_APPLIED', check_metadata(self.ready())['status'])

    def test_missing_target_schema(self):
        md = self.ready()
        del md['schema']
        self.blocked(md)

    def test_wrong_referenced_database(self):
        md = self.ready()
        md['tables']['ipad_batch_request']['foreign_keys'][-1]['ref_schema'] = 'different_database'
        self.blocked(md)

    def test_unknown_referenced_database(self):
        md = self.ready()
        del md['tables']['ipad_batch_request']['foreign_keys'][-1]['ref_schema']
        self.blocked(md)

    def test_uncollected_prefix(self):
        md = self.ready()
        del md['tables']['booking_master']['indexes'][0]['columns'][-1]['prefix']
        self.blocked(md)

    def test_nullable_scope_column(self):
        md = self.ready()
        md['tables']['ipad_batch_request']['columns']['store_id']['nullable'] = True
        self.blocked(md)

    def test_uncollected_nullability(self):
        md = self.ready()
        del md['tables']['booking_master']['columns']['id']['nullable']
        self.blocked(md)

    def test_nullable_parent_scope_with_zero_mismatches(self):
        md = self.ready()
        md['tables']['booking_master']['columns']['store_id']['nullable'] = True
        self.assertEqual('ALREADY_APPLIED', check_metadata(md)['status'])

    def test_cli_unknown_input_nonzero_without_ddl(self):
        # Keep this tiny fixture as evidence; no automatic deletion.
        folder = Path(tempfile.mkdtemp(prefix='co-tl28-guard-'))
        fixture = folder / 'missing-metadata.json'
        fixture.write_text('{}', encoding='utf-8')
        result = subprocess.run([sys.executable, str(Path(__file__).with_name('check.py')), str(fixture)],
                                capture_output=True, text=True, encoding='utf-8',
                                env={**__import__('os').environ, 'PYTHONIOENCODING': 'utf-8'})
        self.assertNotEqual(0, result.returncode)
        self.assertEqual([], json.loads(result.stdout)['ddl'])


if __name__ == '__main__':
    unittest.main()
