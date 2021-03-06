package com.ctrip.xpipe.redis.console.migration.status.migration;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ctrip.xpipe.concurrent.AbstractExceptionLogTask;
import com.ctrip.xpipe.redis.console.migration.command.result.ShardMigrationResult.ShardMigrationStep;
import com.ctrip.xpipe.redis.console.migration.model.MigrationCluster;
import com.ctrip.xpipe.redis.console.migration.model.MigrationShard;
import com.ctrip.xpipe.redis.console.migration.status.MigrationStatus;
import com.ctrip.xpipe.utils.XpipeThreadFactory;

/**
 * @author shyin
 *
 *         Dec 8, 2016
 */
public class MigrationCheckingState extends AbstractMigrationState {

	private ExecutorService executors;

	public MigrationCheckingState(MigrationCluster holder) {
		super(holder, MigrationStatus.Checking);
		this.setNextAfterSuccess(new MigrationMigratingState(holder))
			.setNextAfterFail(this);

		executors = Executors.newCachedThreadPool(XpipeThreadFactory.create("MigrationChecking"));
	}

	@Override
	public void doAction() {
		MigrationCluster migrationCluster = getHolder();
		
		List<MigrationShard> migrationShards = migrationCluster.getMigrationShards();
		for (final MigrationShard migrationShard : migrationShards) {
			executors.submit(new AbstractExceptionLogTask() {
				@Override
				public void doRun() {
					migrationShard.doCheck();
				}
			});
		}
	}

	@Override
	public void refresh() {

		int successCnt = 0;
		List<MigrationShard> migrationShards = getHolder().getMigrationShards();
		for (MigrationShard migrationShard : migrationShards) {
			if (migrationShard.getShardMigrationResult().stepSuccess(ShardMigrationStep.CHECK)) {
				++successCnt;
			}
		}

		if (successCnt == migrationShards.size()) {
			updateAndProcess(nextAfterSuccess(), true);
		}
	}

}
