#!/usr/bin/env python3
"""Evidence-boundary checks for the production baseline reconciliation policy."""
import unittest
from pathlib import Path
from reconcile_baseline_textures import reconcile, runtime_gate


class ReconciliationPolicyTest(unittest.TestCase):
    def run_row(self, current=(), original=(), attempts=(), inactive=(), proof=(), assets=(), skipped=(), markdown=()):
        runtime = {'resourcePacks': {'standard': list(attempts)}, 'inactiveResourceDeclarations': list(inactive),
                   'inactiveTerrainVariants': {'standard': list(skipped)}}
        evidence = {('mod', 'old.png'): list(proof)}
        return reconcile([{'loader_mod': 'mod', 'resource': 'old.png'}], list(current), list(original), runtime,
                         evidence, set(assets), list(markdown))[0]

    def definition(self, path='old.png'):
        return {'source': 'mod-texture.txt', 'id': 'example', 'filename': path, 'loader_mod': 'mod'}

    def test_removal_does_not_prove_a_fix(self):
        self.assertEqual(self.run_row(assets=['old.png'])['status'], 'declaration_removed_needs_review')

    def test_document_name_is_only_a_review_pointer(self):
        row = self.run_row(markdown=[(Path('explanation.md'), 'old.png')])
        self.assertEqual(row['status'], 'declaration_removed_needs_review')
        self.assertEqual(row['reviewDocumentPointers'], ['explanation.md'])

    def test_required_failure_wins_over_success_or_optional_proof(self):
        row = self.run_row(current=[self.definition()], proof=[{'kind': 'optional'}], attempts=[
            {'resource': 'old.png', 'mod': 'mod', 'outcome': 'ORIGINAL'},
            {'resource': 'old.png', 'mod': 'mod', 'outcome': 'REQUIRED_MISSING'}])
        self.assertEqual(row['status'], 'required_missing')

    def test_optional_absence_needs_the_source_contract(self):
        attempts = [{'resource': 'old.png', 'outcome': 'OPTIONAL_ABSENT'}]
        self.assertEqual(self.run_row(attempts=attempts)['status'], 'optional_absent_needs_source_review')
        self.assertEqual(self.run_row(attempts=attempts, proof=[{'kind': 'optional'}])['status'], 'optional_absent_source_verified')

    def test_migration_needs_runtime_and_source(self):
        current, original = [self.definition('new.png')], [self.definition()]
        self.assertEqual(self.run_row(current=current, original=original, proof=[{'kind': 'source_correction'}])['status'], 'replacement_not_runtime_observed')
        attempts = [{'resource': 'new.png', 'mod': 'mod', 'outcome': 'ORIGINAL'}]
        self.assertEqual(self.run_row(current=current, original=original, attempts=attempts)['status'], 'replacement_loaded_needs_source_review')
        self.assertEqual(self.run_row(current=current, original=original, attempts=attempts, proof=[{'kind': 'source_correction'}])['status'], 'replacement_loaded_source_verified')

    def test_inactive_declaration_is_not_automatically_resolved(self):
        row = {'resource': 'old.png', 'mod': 'mod'}
        self.assertEqual(self.run_row(inactive=[row])['status'], 'inactive_declaration_needs_source_review')
        self.assertEqual(self.run_row(inactive=[row], proof=[{'kind': 'registry_gate'}])['status'], 'inactive_declaration_source_verified')

    def test_retirement_requires_explicit_source_evidence(self):
        self.assertEqual(self.run_row(proof=[{'kind': 'retirement'}])['status'], 'retired_source_verified')
        self.assertEqual(self.run_row(proof=[{'kind': 'source_correction'}])['status'], 'declaration_removed_needs_review')

    def test_inactive_terrain_role_and_prelookup_skip_are_separate(self):
        row = self.run_row(attempts=[{'resource': 'old.png', 'outcome': 'INACTIVE_TERRAIN_VARIANT', 'rawOutcome': 'REQUIRED_MISSING'}])
        self.assertEqual(row['status'], 'inactive_terrain_variant')
        row = self.run_row(skipped=['old.png'])
        self.assertEqual(row['status'], 'inactive_terrain_variant')
        self.assertEqual(row['runtimeLookups'], [])
        self.assertEqual(row['skippedRuntimeVariants'][0]['reason'], 'inactive_terrain_variant')

    def test_other_loader_failure_does_not_reopen_retired_consumer(self):
        row = self.run_row(attempts=[{'resource': 'old.png', 'mod': 'other', 'outcome': 'REQUIRED_MISSING'}], proof=[{'kind': 'retirement'}])
        self.assertEqual(row['status'], 'retired_source_verified')

    def test_fallback_requires_source_evidence(self):
        attempts = [{'resource': 'old.png', 'outcome': 'FALLBACK', 'resolvedResource': 'base.png'}]
        self.assertEqual(self.run_row(attempts=attempts)['status'], 'fallback_loaded_needs_source_review')
        self.assertEqual(self.run_row(attempts=attempts, proof=[{'kind': 'generated_fallback'}])['status'], 'fallback_loaded_source_verified')

    def test_asset_existence_does_not_fake_runtime_loading(self):
        row = self.run_row(current=[self.definition()], assets=['old.png'])
        self.assertEqual(row['status'], 'declared_not_runtime_observed')
        self.assertTrue(row['originalAssetExists'])

    def test_runtime_gate_keeps_global_and_parser_failures(self):
        runtime = {'schemaVersion': 2, 'resourceCoverage': 'LOADED_PACKS_ONLY', 'summary': {
            'requiredMissingResources': 1, 'textureDefinitionErrors': 1, 'failedCustomModelBlocks': 0,
            'unmappedMtes': 0, 'mappedMtesWithoutBase': 0, 'unresolvedMteIconSets': 0}}
        passed, failures, unreported = runtime_gate(runtime)
        self.assertFalse(passed)
        self.assertEqual(failures, {'requiredMissingResources': 1, 'textureDefinitionErrors': 1})
        self.assertEqual(unreported, [])
        runtime['summary']['requiredMissingResources'] = 0
        runtime['summary']['textureDefinitionErrors'] = 0
        self.assertTrue(runtime_gate(runtime)[0])

    def test_unreported_checks_do_not_fabricate_a_clean_runtime(self):
        passed, failures, unreported = runtime_gate({'schemaVersion': 2, 'resourceCoverage': 'LOADED_PACKS_ONLY'})
        self.assertFalse(passed)
        self.assertTrue(unreported)

    def test_texture_ids_are_file_qualified(self):
        other = dict(self.definition('wrong.png'), source='other-texture.txt', loader_mod='other')
        row = self.run_row(current=[other], original=[self.definition()])
        self.assertEqual(row['replacementResources'], [])


if __name__ == '__main__':
    unittest.main()
